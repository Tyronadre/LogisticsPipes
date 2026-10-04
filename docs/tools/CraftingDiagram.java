import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Source-only AST inventory. Run from the repository root with JDK 17 or newer. */
public class CraftingDiagram {
    record Type(String id, String name, String file, long line, List<Method> methods, Set<String> refs) {}
    record Method(String id, Type owner, String name, String signature, long line, List<String> calls) {}
    static final List<Type> types = new ArrayList<>();
    static final List<Method> methods = new ArrayList<>();
    static final List<String> files = new ArrayList<>();
    static String xml(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    static String graphStart() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" xmlns:y=\"http://www.yworks.com/xml/graphml\">\n" +
            "<key id=\"graphics\" for=\"node\" yfiles.type=\"nodegraphics\"/>\n" +
            "<key id=\"label\" for=\"all\" attr.name=\"label\" attr.type=\"string\"/>\n" +
            "<key id=\"source\" for=\"node\" attr.name=\"source\" attr.type=\"string\"/>\n" +
            "<key id=\"relation\" for=\"edge\" attr.name=\"relation\" attr.type=\"string\"/>\n" +
            "<graph id=\"crafting\" edgedefault=\"directed\">\n";
    }
    static void node(StringBuilder out, String id, String label, String source, String color) {
        int rows = label.split("\n").length;
        out.append("<node id=\"").append(id).append("\"><data key=\"label\">").append(xml(label))
            .append("</data><data key=\"source\">").append(xml(source)).append("</data><data key=\"graphics\"><y:ShapeNode>")
            .append("<y:Geometry width=\"760\" height=\"").append(Math.max(50, rows * 17 + 20)).append("\"/>")
            .append("<y:Fill color=\"").append(color).append("\" transparent=\"false\"/>")
            .append("<y:NodeLabel alignment=\"left\" fontSize=\"12\">").append(xml(label))
            .append("</y:NodeLabel><y:Shape type=\"roundrectangle\"/></y:ShapeNode></data></node>\n");
    }
    static void edge(StringBuilder out, String from, String to, String relation) {
        out.append("<edge source=\"").append(from).append("\" target=\"").append(to)
            .append("\"><data key=\"relation\">").append(xml(relation)).append("</data><data key=\"label\">")
            .append(xml(relation)).append("</data></edge>\n");
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of("src/main/java/logisticspipes");
        try (var paths = Files.walk(root.resolve("crafting"))) {
            paths.filter(p -> p.toString().endsWith(".java") && !p.toString().contains("requesttable"))
                .forEach(p -> files.add(p.toString().replace('\\', '/')));
        }
        try (var paths = Files.walk(root.resolve("gui/modularUI/pipes"))) {
            paths.filter(p -> p.toString().endsWith(".java") &&
                (p.toString().contains("patterncrafting") || p.toString().toLowerCase().contains("satellite")))
                .forEach(p -> files.add(p.toString().replace('\\', '/')));
        }
        files.add(root + "/pipes/PipeItemsPatternCraftingLogistics.java");
        files.add(root + "/pipes/upgrades/PatternUpgrade.java");
        files.add(root + "/network/packets/orderer/PatternCraftingWatchPacket.java");
        // Shared LP integration classes are included in full, including their older methods.
        for (String integration : List.of(
                "gui/popup/PatternRequestMonitorPopup.java", "nei/PatternRecipeImporter.java",
                "renderer/PatternItemRenderer.java", "pipes/upgrades/InstantSatelliteUpgrade.java",
                "pipes/upgrades/CraftingMonitoringUpgrade.java", "modules/ModuleProvider.java",
                "pipes/PipeItemsProviderLogistics.java", "routing/ItemRoutingInformation.java",
                "request/RequestTree.java", "request/RequestTreeNode.java",
                "request/debug/CraftingRequestDebugManager.java")) files.add(root + "/" + integration);
        Collections.sort(files);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null,
                manager.getJavaFileObjectsFromStrings(files));
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree unit : task.parse()) {
                String file = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().toString();
                file = Path.of("").toAbsolutePath().relativize(Path.of(file)).toString().replace('\\', '/');
                final String source = file;
                new TreeScanner<Void, Void>() {
                    Type current;
                    Method method;
                    long line(Tree tree) { return unit.getLineMap().getLineNumber(trees.getSourcePositions().getStartPosition(unit, tree)); }
                    @Override public Void visitClass(ClassTree tree, Void unused) {
                        Type previous = current;
                        Method previousMethod = method;
                        String simple = tree.getSimpleName().toString();
                        if (simple.isEmpty()) simple = "anonymous@" + line(tree);
                        String name = previous == null ? unit.getPackageName() + "." + simple : previous.name() + "." + simple;
                        current = new Type("c" + types.size(), name, source, line(tree), new ArrayList<>(), new TreeSet<>());
                        types.add(current);
                        method = null;
                        super.visitClass(tree, unused);
                        current = previous;
                        method = previousMethod;
                        return null;
                    }
                    @Override public Void visitMethod(MethodTree tree, Void unused) {
                        Method previous = method;
                        String name = tree.getName().toString();
                        String signature = tree.getModifiers() + " " + (tree.getReturnType() == null ? "" : tree.getReturnType() + " ") +
                            name + "(" + tree.getParameters().stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
                        signature = signature.replaceAll("\\s+", " ").trim();
                        method = new Method("m" + methods.size(), current, name, signature, line(tree), new ArrayList<>());
                        methods.add(method);
                        current.methods().add(method);
                        super.visitMethod(tree, unused);
                        method = previous;
                        return null;
                    }
                    @Override public Void visitIdentifier(IdentifierTree tree, Void unused) {
                        if (current != null) current.refs().add(tree.getName().toString());
                        return super.visitIdentifier(tree, unused);
                    }
                    @Override public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                        if (method != null) method.calls().add(tree.getMethodSelect().toString());
                        return super.visitMethodInvocation(tree, unused);
                    }
                    @Override public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
                        if (method != null) method.calls().add(tree.getQualifierExpression() + "." + tree.getName());
                        return super.visitMemberReference(tree, unused);
                    }
                    @Override public Void visitNewClass(NewClassTree tree, Void unused) {
                        if (method != null) method.calls().add("new " + tree.getIdentifier());
                        return super.visitNewClass(tree, unused);
                    }
                }.scan(unit, null);
            }
        }
        StringBuilder classes = new StringBuilder(graphStart());
        for (Type type : types) {
            String label = type.name() + "\n" + type.file() + ":" + type.line() + "\n" +
                type.methods().stream().map(m -> m.signature() + " [L" + m.line() + "]").collect(Collectors.joining("\n"));
            node(classes, type.id(), label, type.file() + ":" + type.line(), "#DCEEFF");
        }
        for (Type from : types) for (Type to : types) {
            String simple = to.name().substring(to.name().lastIndexOf('.') + 1);
            if (from != to && from.refs().contains(simple)) edge(classes, from.id(), to.id(), "source type/name reference");
        }
        classes.append("</graph></graphml>\n");
        StringBuilder calls = new StringBuilder(graphStart());
        for (Type type : types) node(calls, type.id(), type.name(), type.file() + ":" + type.line(), "#DCEEFF");
        for (Method m : methods) {
            node(calls, m.id(), m.owner().name() + "\n" + m.signature(), m.owner().file() + ":" + m.line(), "#E7F5DF");
            edge(calls, m.owner().id(), m.id(), "declares");
            for (String call : new TreeSet<>(m.calls())) {
                boolean constructor = call.startsWith("new ");
                String name = call.substring(call.lastIndexOf('.') + 1);
                if (constructor) name = "<init>";
                final String calledName = name;
                for (Method target : methods) {
                    if (!target.name().equals(calledName)) continue;
                    if (constructor && !call.contains(target.owner().name().substring(target.owner().name().lastIndexOf('.') + 1))) continue;
                    // Candidate edges deliberately avoid claiming semantic overload/interface resolution.
                    edge(calls, m.id(), target.id(), "lexical call candidate: " + call);
                }
            }
        }
        calls.append("</graph></graphml>\n");
        Path docs = Path.of("docs");
        Files.writeString(docs.resolve("pattern-crafting-classes.graphml"), classes);
        Files.writeString(docs.resolve("pattern-crafting-functions.graphml"), calls);
        StringBuilder inventory = new StringBuilder("# Complete pattern crafting source inventory\n\n");
        inventory.append("Generated from Java syntax trees: ").append(files.size()).append(" source files, ")
            .append(types.size()).append(" named/nested/anonymous types, ").append(methods.size()).append(" explicit methods and constructors.\n\n")
            .append("Includes the crafting package (except the separate requesttable UI), pattern data/stack types, pattern pipe, pattern editor and satellite GUIs, monitor/popup, upgrades, importer, renderer, and watch packet. Shared integration classes are included in full: RequestTree, RequestTreeNode, ModuleProvider, PipeItemsProviderLogistics, ItemRoutingInformation, and CraftingRequestDebugManager. Other external LP/Minecraft/GT APIs are outside the graph. Lombok-generated methods and implicit constructors are not source declarations; lambdas are included in their enclosing method's call list. Calls in field/static initializers are outside the method graph.\n\n")
            .append("- [Class diagram](pattern-crafting-classes.graphml): every type, with every declared method in its label. Edges are lexical source references, not verified dependencies.\n")
            .append("- [Function diagram](pattern-crafting-functions.graphml): every explicit method/constructor and its owning type. Call edges are name-based candidates, including method references and constructor calls; ambiguous receivers/overloads can produce multiple edges. These are not a semantically resolved call graph.\n")
            .append("- [Runtime process](pattern-crafting-process.md): manually traced execution flow.\n\n")
            .append("Open GraphML in yEd and apply a hierarchical or organic layout. Search/filter the function graph by class; it is too large to read at once. Node source attributes include repository-relative paths and declaration lines.\n\n")
            .append("Regenerate from the repository root with `java docs/tools/CraftingDiagram.java` (JDK 17+). Parsing needs no Minecraft dependencies and performs no compilation or tests.\n\n");
        for (Type type : types) {
            inventory.append("## ").append(type.name()).append("\n\nSource: `").append(type.file()).append(":").append(type.line()).append("`\n\n");
            for (Method m : type.methods()) inventory.append("- `").append(m.signature()).append("` — line ").append(m.line()).append("\n");
            if (type.methods().isEmpty()) inventory.append("No explicit method declarations.\n");
            inventory.append("\n");
        }
        Files.writeString(docs.resolve("pattern-crafting-code-map.md"), inventory.toString().stripTrailing() + "\n");
        System.out.println(files.size() + " files; " + types.size() + " types; " + methods.size() + " methods/constructors");
    }
}
