package logisticspipes.crafting;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Stable identity for one object owned by a pattern-crafting instance.
 *
 * <p>
 * The instance id groups the complete recursive request. The object id distinguishes orders, deliveries, buffered
 * ownership records and satellite batches inside that instance. Children retain their parent's object id; it is lineage
 * metadata and does not change identity comparisons.
 * </p>
 */
public final class PatternCraftingReference {

    private static final String INSTANCE_SUFFIX = "InstanceId";
    private static final String OBJECT_SUFFIX = "ObjectId";
    private static final String PARENT_SUFFIX = "ParentId";

    private final UUID instanceId;
    private final UUID objectId;
    private final UUID parentId;

    private PatternCraftingReference(UUID instanceId, UUID objectId) {
        this(instanceId, objectId, null);
    }

    private PatternCraftingReference(UUID instanceId, UUID objectId, UUID parentId) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.objectId = Objects.requireNonNull(objectId, "objectId");
        this.parentId = parentId;
    }

    public static PatternCraftingReference createInstance() {
        UUID instanceId = UUID.randomUUID();
        return new PatternCraftingReference(instanceId, UUID.randomUUID());
    }

    public static PatternCraftingReference createObject(UUID instanceId) {
        return new PatternCraftingReference(instanceId, UUID.randomUUID());
    }

    public static PatternCraftingReference readFromNBT(NBTTagCompound tag, String prefix) {
        String instance = tag.getString(prefix + INSTANCE_SUFFIX);
        String object = tag.getString(prefix + OBJECT_SUFFIX);
        if (instance.isEmpty() || object.isEmpty()) {
            return null;
        }
        try {
            String parent = tag.getString(prefix + PARENT_SUFFIX);
            return new PatternCraftingReference(
                    UUID.fromString(instance),
                    UUID.fromString(object),
                    parent.isEmpty() ? null : UUID.fromString(parent));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public PatternCraftingReference createChild() {
        return new PatternCraftingReference(instanceId, UUID.randomUUID(), objectId);
    }

    public UUID parentId() {
        return parentId;
    }

    public PatternCraftingReference parent() {
        return parentId == null ? null : new PatternCraftingReference(instanceId, parentId);
    }

    public UUID instanceId() {
        return instanceId;
    }

    public UUID objectId() {
        return objectId;
    }

    public boolean belongsTo(PatternCraftingReference other) {
        return other != null && instanceId.equals(other.instanceId);
    }

    public void writeToNBT(NBTTagCompound tag, String prefix) {
        tag.setString(prefix + INSTANCE_SUFFIX, instanceId.toString());
        tag.setString(prefix + OBJECT_SUFFIX, objectId.toString());
        if (parentId != null) {
            tag.setString(prefix + PARENT_SUFFIX, parentId.toString());
        } else {
            tag.removeTag(prefix + PARENT_SUFFIX);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PatternCraftingReference other)) {
            return false;
        }
        return instanceId.equals(other.instanceId) && objectId.equals(other.objectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, objectId);
    }

    @Override
    public String toString() {
        return instanceId.toString().substring(0, 8) + "/" + objectId.toString().substring(0, 8);
    }
}
