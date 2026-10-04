# Complete pattern crafting source inventory

Generated from Java syntax trees: 96 source files, 150 named/nested/anonymous types, 1857 explicit methods and constructors.

Includes the crafting package (except the separate requesttable UI), pattern data/stack types, pattern pipe, pattern editor and satellite GUIs, monitor/popup, upgrades, importer, renderer, and watch packet. Shared integration classes are included in full: RequestTree, RequestTreeNode, ModuleProvider, PipeItemsProviderLogistics, ItemRoutingInformation, and CraftingRequestDebugManager. Other external LP/Minecraft/GT APIs are outside the graph. Lombok-generated methods and implicit constructors are not source declarations; lambdas are included in their enclosing method's call list. Calls in field/static initializers are outside the method graph.

- [Class diagram](pattern-crafting-classes.graphml): every type, with every declared method in its label. Edges are lexical source references, not verified dependencies.
- [Function diagram](pattern-crafting-functions.graphml): every explicit method/constructor and its owning type. Call edges are name-based candidates, including method references and constructor calls; ambiguous receivers/overloads can produce multiple edges. These are not a semantically resolved call graph.
- [Runtime process](pattern-crafting-process.md): manually traced execution flow.

Open GraphML in yEd and apply a hierarchical or organic layout. Search/filter the function graph by class; it is too large to read at once. Node source attributes include repository-relative paths and declaration lines.

Regenerate from the repository root with `java docs/tools/CraftingDiagram.java` (JDK 17+). Parsing needs no Minecraft dependencies and performs no compilation or tests.

## logisticspipes.crafting.AdjacentInventoryHandler

Source: `src/main/java/logisticspipes/crafting/AdjacentInventoryHandler.java:34`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe)` — line 45
- `void invalidate()` — line 50
- `AdjacentTile getConnected()` — line 57
- `boolean isConnectedToPatternCraftingTable()` — line 61
- `public boolean hasConnectedTE()` — line 66
- `List<AdjacentTile> locateFluidHandlers()` — line 71
- `int roomFor(AdjacentTile connected, ItemIdentifier item)` — line 80
- `int availablePatternSets(ItemStack pattern)` — line 89
- `boolean canInsertPatternIngredients(ItemStack pattern, List<PatternIngredientAssignment> assignments)` — line 143
- `int[] insertPatternIngredients(ItemStack pattern, List<PatternIngredientAssignment> assignments)` — line 198
- `private int availablePatternSetsForFluids(List<PatternFluidStack> ingredients, AdjacentTile connected)` — line 226
- `private boolean canFitFluids(IFluidHandler handler, ForgeDirection side, List<PatternFluidStack> fluids, int sets)` — line 276
- `static boolean canFitFluids(List<Pair<IFluidHandler, ForgeDirection>> handlers, List<PatternFluidStack> fluids, int sets)` — line 281
- `private static List<PatternFluidStack> mergeFluids(List<PatternFluidStack> fluids, int sets)` — line 369
- `private List<ItemIdentifierStack> getSolidIngredients(List<IPatternStack> ingredients)` — line 389
- `private List<PatternFluidStack> getFluidIngredients(List<IPatternStack> ingredients)` — line 400
- `private int availablePatternSetsDisregardingSlots(List<ItemIdentifierStack> ingredients, AdjacentTile connected)` — line 410
- `private IInventory getInsertableInventory(AdjacentTile connected)` — line 441
- `static boolean canFitPatternSetsDisregardingSlots(IInventory inventory, List<ItemIdentifierStack> ingredients, int sets)` — line 452
- `private static boolean insertIntoSnapshot(IInventory inventory, ItemStack[] snapshot, ItemStack stack)` — line 479
- `private int availablePatternSetsForPatternTable(ItemStack pattern, PatternLogisticsCraftingTableTileEntity table)` — line 516
- `private int insert(ItemIdentifierStack item)` — line 547
- `private int insertFluid(PatternFluidStack fluid)` — line 592
- `private ForgeDirection getFluidInsertionOrientation(AdjacentTile connected)` — line 606
- `boolean isEmpty(AdjacentTile connected)` — line 610
- `private boolean calculateEmpty(AdjacentTile connected)` — line 624
- `ItemStack extract(IResource wanted, int count)` — line 650
- `ItemStack extract(AdjacentTile tile, IResource wanted, int count)` — line 654
- `FluidStack extractFluid(AdjacentTile tile, PatternFluidStack wanted, int amount)` — line 703
- `public List<ItemStack> getExtractableItems()` — line 740
- `private void refreshContentCache(AdjacentTile connected)` — line 770
- `private void invalidateContentCache()` — line 783
- `private void clearContentCacheValues()` — line 787

## logisticspipes.crafting.AdjacentInventoryHandler.FluidCapacitySnapshot

Source: `src/main/java/logisticspipes/crafting/AdjacentInventoryHandler.java:340`

- `private <init>(IFluidHandler handler, ForgeDirection side)` — line 347

## logisticspipes.crafting.CraftingMonitorGui

Source: `src/main/java/logisticspipes/crafting/CraftingMonitorGui.java:28`

- `public <init>(CraftingMonitorTileEntity tile, List<PatternCraftingMonitorEntry> entries)` — line 45
- `@Override public void initGui()` — line 51
- `@Override public void updateScreen()` — line 59
- `@Override protected void actionPerformed(GuiButton button)` — line 68
- `@Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)` — line 91
- `@Override public void drawScreen(int mouseX, int mouseY, float partialTicks)` — line 106
- `@Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton)` — line 114
- `public void handleContent(List<PatternCraftingMonitorEntry> updatedEntries)` — line 129
- `public boolean isFor(CraftingMonitorTileEntity otherTile)` — line 148
- `private void rebuildButtons()` — line 154
- `private void drawRequests(int mouseX, int mouseY)` — line 178
- `private void drawSelectedRecipe(int mouseX, int mouseY)` — line 222
- `private void renderStack(ItemIdentifierStack identifierStack, int x, int y, int mouseX, int mouseY, List<String> details)` — line 278
- `private void handleMouseWheel(int mouseX, int mouseY)` — line 299
- `private void scrollRequests(int direction)` — line 312
- `private void scrollDetails(int direction)` — line 320
- `private int maxRequestScroll()` — line 324
- `private int maxDetailScroll()` — line 328
- `private List<NodeRow> selectedRows()` — line 332
- `private void flatten(PatternCraftingMonitorNode node, int depth, List<NodeRow> rows)` — line 343
- `private int findEntry(UUID instanceId)` — line 350
- `private void requestRefresh()` — line 361

## logisticspipes.crafting.CraftingMonitorGui.NodeRow

Source: `src/main/java/logisticspipes/crafting/CraftingMonitorGui.java:365`

- `private <init>(PatternCraftingMonitorNode node, int depth)` — line 370

## logisticspipes.crafting.CraftingMonitorGuiProvider

Source: `src/main/java/logisticspipes/crafting/CraftingMonitorGuiProvider.java:19`

- `public <init>(int id)` — line 26
- `@Override public Object getClientGui(EntityPlayer player)` — line 30
- `@Override public Container getContainer(EntityPlayer player)` — line 41
- `@Override public GuiProvider template()` — line 47
- `@Override public void writeData(LPDataOutputStream data)` — line 52
- `@Override public void readData(LPDataInputStream data)` — line 61

## logisticspipes.crafting.CraftingMonitorTileEntity

Source: `src/main/java/logisticspipes/crafting/CraftingMonitorTileEntity.java:19`

- `@Override public void notifyOfBlockChange()` — line 23
- `@Override public CoordinatesGuiProvider getGuiProvider()` — line 28
- `public List<PatternCraftingMonitorEntry> getMonitorEntries()` — line 33
- `public boolean cancel(UUID instanceId)` — line 41
- `public CoreRoutedPipe getConnectedPipe()` — line 46

## logisticspipes.crafting.IPatternSatellitePipe

Source: `src/main/java/logisticspipes/crafting/IPatternSatellitePipe.java:8`

- `String getSatelliteName()` — line 13
- `void setSatelliteName(String satelliteName)` — line 18

## logisticspipes.crafting.IStagedCraftingProvider

Source: `src/main/java/logisticspipes/crafting/IStagedCraftingProvider.java:8`

- `IOrderInfoProvider fullFillStagedCrafting(IPromise promise, IResource requestType, IAdditionalTargetInformation info, PatternCraftingBranch branch)` — line 10

## logisticspipes.crafting.IStagedProviderReservation

Source: `src/main/java/logisticspipes/crafting/IStagedProviderReservation.java:5`

- `void reserveStagedCrafting(ItemIdentifier item, int amount)` — line 10
- `void releaseStagedCrafting(ItemIdentifier item, int amount)` — line 15

## logisticspipes.crafting.ItemMemoryChip

Source: `src/main/java/logisticspipes/crafting/ItemMemoryChip.java:21`

- `public static int[] getPatternSatelliteIds(ItemStack stack)` — line 44
- `public static List<StoredPatternSatellite> getPatternSatellites(ItemStack stack)` — line 51
- `public static PatternSatelliteMode getPatternSatelliteMode(ItemStack stack)` — line 74
- `public static PatternSatelliteMode cyclePatternSatelliteMode(ItemStack stack)` — line 83
- `@Override public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player)` — line 90
- `@Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)` — line 95
- `public static boolean addPatternSatellite(ItemStack stack, PipeItemsPatternSatelliteLogistics satellite)` — line 107
- `public static boolean addPatternSatellite(ItemStack stack, int satelliteId, String satelliteUuid, String satelliteName)` — line 118
- `public static boolean addPatternSatelliteId(ItemStack stack, int satelliteId)` — line 143
- `public static int getLastPatternSatelliteId(ItemStack stack)` — line 147
- `public static String getLastPatternSatelliteUuid(ItemStack stack)` — line 154
- `public static String getLastPatternSatelliteName(ItemStack stack)` — line 161
- `private static void writePatternSatellites(NBTTagCompound root, List<StoredPatternSatellite> satellites)` — line 168
- `@Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)` — line 191
- `private static String formatSatellites(List<StoredPatternSatellite> satellites)` — line 209
- `private static String formatMode(PatternSatelliteMode mode)` — line 218
- `private static NBTTagCompound getOrCreateTag(ItemStack stack)` — line 225

## logisticspipes.crafting.ItemMemoryChip.PatternSatelliteMode

Source: `src/main/java/logisticspipes/crafting/ItemMemoryChip.java:30`

No explicit method declarations.

## logisticspipes.crafting.ItemMemoryChip.StoredPatternSatellite

Source: `src/main/java/logisticspipes/crafting/ItemMemoryChip.java:35`

- `public <init>(int id, String uuid, String name)` — line 38

## logisticspipes.crafting.ModulePatternCrafting

Source: `src/main/java/logisticspipes/crafting/ModulePatternCrafting.java:70`

- `public <init>(PipeItemsPatternCraftingLogistics pipe)` — line 112
- `public IInventory getPatternInventory()` — line 185
- `public ItemStack getPatternStack(int slot)` — line 189
- `PatternRecipeSnapshot getPatternRecipe(ItemStack pattern)` — line 193
- `PatternCraftingBatchOutputs batchOutputs()` — line 197
- `PatternCraftingWorkspace workspace()` — line 201
- `long unreservedInputs(Set<UUID> admitted, boolean fluid)` — line 205
- `boolean hasWorkspaceWork(UUID instance)` — line 210
- `boolean hasNonBatchWorkspaceWork(UUID instance)` — line 214
- `boolean hasLegacyOrders()` — line 228
- `public ItemStack getPatternItemStack(int slot)` — line 235
- `public void markPatternInventoryDirty()` — line 242
- `public void onCraftingTargetChanged()` — line 247
- `public int assignSatelliteToAllPatternIngredients(int satelliteId, String satelliteUuid)` — line 253
- `public PipeItemsPatternCraftingLogistics.BlockingMode getBlockingMode()` — line 279
- `public void setBlockingMode(PipeItemsPatternCraftingLogistics.BlockingMode blockingMode)` — line 283
- `public boolean isBlockingModeFixed()` — line 296
- `PipeItemsPatternCraftingLogistics.BlockingMode getEffectiveBlockingMode()` — line 300
- `@Override public SinkReply sinksItem(ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit)` — line 314
- `public int sinkAmount(FluidStack stack)` — line 356
- `@Override public void tick()` — line 369
- `@Override public boolean hasGenericInterests()` — line 384
- `@Override public Collection<ItemIdentifier> getSpecificInterests()` — line 392
- `public Set<ItemIdentifier> getCraftedItems()` — line 400
- `public Set<ItemIdentifier> getOutputItems()` — line 404
- `@Override public boolean interestedInAttachedInventory()` — line 411
- `@Override public boolean interestedInUndamagedID()` — line 416
- `@Override public boolean recievePassive()` — line 421
- `@Override public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter)` — line 426
- `@Override public LogisticsModule getSubModule(int slot)` — line 429
- `@Override public int getX()` — line 434
- `@Override public int getY()` — line 439
- `@Override public int getZ()` — line 444
- `@Override public Map<FluidIdentifier, Integer> getAvailableFluids()` — line 449
- `@Override public net.minecraft.util.IIcon getIconTexture(IIconRegister register)` — line 454
- `@Override public void readFromNBT(NBTTagCompound tag)` — line 459
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 509
- `@Override public void registerPosition(ModulePositionType slot, int positionInt)` — line 549
- `void debug(String message, Object... args)` — line 555
- `public void debugEvent(String category, String message, Object... args)` — line 561
- `void debugEventThrottled(String category, String message, Object... args)` — line 565
- `void debugEventThrottled(String category, int intervalTicks, String message, Object... args)` — line 569
- `public void recordDebugEvent(String category, String message)` — line 593
- `private String formatDebugMessage(String message, Object... args)` — line 598
- `private long currentDebugTick()` — line 617
- `long currentWorldTick()` — line 621
- `private void restoreStagedCraftingIfNeeded()` — line 627
- `List<PatternCraftingMonitorEntry> getPendingRestoreEntries()` — line 650
- `List<PatternCraftingMonitorEntry> getStandaloneOrderEntries()` — line 658
- `private boolean appendStandaloneOrder(LogisticsOrder order, Map<UUID, List<PatternCraftingMonitorNode>> rootsByInstance)` — line 679
- `boolean hasStandaloneOrderInstance(UUID instanceId)` — line 705
- `boolean cancelStandaloneOrderInstance(UUID instanceId)` — line 722
- `private boolean isStandaloneOrderForInstance(LogisticsOrder order, UUID instanceId)` — line 736
- `boolean hasPendingRestoreInstance(UUID instanceId)` — line 743
- `boolean cancelPendingRestore(UUID instanceId)` — line 748
- `private void markPersistentStateDirty()` — line 765
- `void markCraftingStateDirty()` — line 771
- `int incomingCraftingAmount(PatternCraftingReference owner, IPatternStack ingredient)` — line 777
- `private int incomingOrderAmount(LogisticsOrder order, PatternCraftingReference owner, IPatternStack ingredient)` — line 789
- `void deferDispatchCleanup(PatternSatelliteDispatchHandler.DispatchPlan plan, boolean returnToStorage)` — line 797
- `private void scheduleRequestedIngredientRestoreRetriesIfReady()` — line 801
- `boolean supportsFluidCrafting()` — line 819
- `boolean hasAdvancedSatelliteUpgrade()` — line 824
- `boolean hasInstantSatelliteUpgrade()` — line 829
- `public boolean isPatternCraftingSupported(ItemStack pattern)` — line 837
- `private boolean isFluidCraftingPattern(ItemStack pattern)` — line 841
- `private void cancelUnsupportedFluidPatternCrafts()` — line 846
- `@Override public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters)` — line 877
- `@Override public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info)` — line 937
- `@Override public IOrderInfoProvider fullFillStagedCrafting(IPromise promise, IResource requestType, IAdditionalTargetInformation info, PatternCraftingBranch branch)` — line 980
- `@Override public IOrderInfoProvider fullFill(FluidLogisticsPromise promise, IRequestFluid destination, ResourceType type, IAdditionalTargetInformation info)` — line 993
- `@Override public void sendFailed(FluidIdentifier fluid, Integer amount)` — line 1022
- `@Override public IRouter getRouter()` — line 1027
- `@Override public void itemCouldNotBeSend(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 1032
- `@Override public int getID()` — line 1037
- `@Override public int compareTo(IRequest request)` — line 1042
- `@Override public void registerExtras(IPromise promise)` — line 1053
- `void registerExtras(IPromise promise, PatternCraftingReference owner)` — line 1058
- `private PatternByproductTarget byproductTarget(IPromise promise)` — line 1097
- `@Override public ICraftingTemplate addCrafting(IResource toCraft)` — line 1108
- `@Override public boolean canCraft(IResource toCraft)` — line 1114
- `@Override public int getTodo()` — line 1124
- `@Override public List<ItemIdentifierStack> getConfiguredCraftResults()` — line 1129
- `public PatternCraftingHudState getHudState()` — line 1138
- `public boolean shouldRefreshHudState()` — line 1145
- `public void markHudStateDirty()` — line 1152
- `public void appendDebugState(StringBuilder out)` — line 1159
- `@Override public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 1178
- `@Override public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 1194
- `protected ISlotUpgradeManager getUpgradeManager()` — line 1200
- `ForgeDirection getInsertionOrientation(AdjacentTile tile)` — line 1211
- `boolean abandonPendingDispatch(UUID instanceId)` — line 1222
- `int pendingDispatchSlot()` — line 1229
- `int getRunningCraftForHandler()` — line 1233
- `boolean canReceiveForPattern(int patternSlot)` — line 1242
- `int maxDispatchablePatternSets(PatternCraftingReference ownerReference, ItemStack pattern, int maxSets)` — line 1253
- `List<IPatternStack> getAggregatedIngredients(ItemStack pattern)` — line 1261
- `List<IPatternStack> getLocalAggregatedIngredients(ItemStack pattern)` — line 1268
- `List<PatternIngredientTarget> getIngredientTargets(ItemStack pattern)` — line 1278
- `int bufferedIngredientAmount(int patternSlot, ItemStack pattern, IPatternStack ingredient)` — line 1282
- `int requestedIngredientAmount(int patternSlot, ItemStack pattern, IPatternStack ingredient)` — line 1289
- `int requestedItemAmount(int patternSlot, ItemStack pattern, ItemIdentifier item)` — line 1293
- `int requestedItemAmount(PatternCraftingReference owner, int patternSlot, ItemIdentifier item)` — line 1297
- `List<PatternIngredientAssignment> buildBufferedIngredientPlan(int patternSlot, ItemStack pattern, int sets)` — line 1301
- `List<PatternIngredientAssignment> buildBufferedIngredientPlan(PatternCraftingReference owner, int patternSlot, ItemStack pattern, int sets)` — line 1305
- `boolean hasLinkedSatelliteAssignment(ItemStack pattern, int inputSlot)` — line 1313
- `boolean hasLinkedSatelliteAssignments(ItemStack pattern)` — line 1320
- `IRequestItems getSatelliteTargetForInputSlot(AbstractPattern pattern, int inputSlot)` — line 1327
- `IRequestFluid getFluidSatelliteTargetForInputSlot(AbstractPattern pattern, int inputSlot)` — line 1335
- `private void pushBufferedIngredients()` — line 1346
- `void pushBufferedIngredientsFor(int patternSlot)` — line 1353
- `int completeBufferedSets(int patternSlot)` — line 1361
- `int completeBufferedSets(PatternCraftingReference owner, int patternSlot)` — line 1365
- `void requestIngredientsForStagedCrafts()` — line 1373
- `public boolean cancelPatternCraft(int patternSlot)` — line 1389
- `boolean cancelTrackedOrder(PatternCraftingOrder order)` — line 1397
- `public boolean returnStoredInputsToStorage()` — line 1408
- `int findCompleteBufferedPattern()` — line 1415
- `void activateRunningCraftFromBuffer(int patternSlot, PatternCraftingReference owner)` — line 1422
- `PatternCraftingReference completeBufferOwner(int patternSlot)` — line 1426
- `boolean isRunningCraftLocked()` — line 1433
- `private void clearRunningCraftIfFinished()` — line 1440
- `AdjacentTile getConnectedInventoryTile()` — line 1444
- `boolean isInventoryEmpty(AdjacentTile connected)` — line 1448
- `private boolean areAllOrdersBuffered()` — line 1460
- `boolean isOrderDestinationThisModule(LogisticsItemOrder order)` — line 1507
- `boolean isOrderDestinationThisModule(LogisticsFluidOrder order)` — line 1512
- `int requestedSamePipeItemAmount(LogisticsItemOrder order)` — line 1517
- `int requestedSamePipeFluidAmount(LogisticsFluidOrder order)` — line 1526
- `private void appendConnectedInventoryDebug(StringBuilder out)` — line 1535
- `private void appendPatternDebug(StringBuilder out)` — line 1550
- `private void appendPatternSlots(StringBuilder out, AbstractPattern pattern, int start, int end, String label)` — line 1574
- `private void appendStackMapDebug(StringBuilder out, String label, Map<Integer, List<IPatternStack>> stacksByPattern)` — line 1594
- `private void appendStagedCraftDebug(StringBuilder out)` — line 1608
- `private void appendOrderDebug(StringBuilder out)` — line 1618
- `private void appendInlineStacks(StringBuilder out, List<IPatternStack> stacks)` — line 1636
- `public void onAllowedRemoval()` — line 1650

## logisticspipes.crafting.ModulePatternCrafting.ThrottledDebugEvent

Source: `src/main/java/logisticspipes/crafting/ModulePatternCrafting.java:1666`

No explicit method declarations.

## logisticspipes.crafting.PatternByproductExtractionResult

Source: `src/main/java/logisticspipes/crafting/PatternByproductExtractionResult.java:8`

- `<init>(int amount, IRoutedItem routedItem)` — line 15
- `static PatternByproductExtractionResult empty()` — line 20
- `int amount()` — line 24
- `IRoutedItem routedItem()` — line 28

## logisticspipes.crafting.PatternByproductExtractionTarget

Source: `src/main/java/logisticspipes/crafting/PatternByproductExtractionTarget.java:11`

- `boolean canExtractByproductsFor(IRouter requester)` — line 16
- `PatternByproductExtractionResult extractItemByproduct(ItemIdentifier item, int amount, int destination, IAdditionalTargetInformation info)` — line 21
- `PatternByproductExtractionResult extractFluidByproduct(FluidIdentifier fluid, int amount, int destination, IAdditionalTargetInformation info)` — line 27

## logisticspipes.crafting.PatternByproductExtractionTargetCache

Source: `src/main/java/logisticspipes/crafting/PatternByproductExtractionTargetCache.java:15`

- `<init>(PipeItemsPatternCraftingLogistics pipe)` — line 22
- `PatternByproductExtractionResult extractItem(PatternByproductTarget configuredTarget, ItemIdentifier item, int amount, int destination, IAdditionalTargetInformation info)` — line 26
- `PatternByproductExtractionResult extractFluid(PatternByproductTarget configuredTarget, FluidIdentifier fluid, int amount, int destination, IAdditionalTargetInformation info)` — line 36
- `PatternByproductExtractionTarget resolve(PatternByproductTarget configuredTarget)` — line 46
- `private PatternByproductExtractionTarget find(PatternByproductTarget configuredTarget)` — line 70

## logisticspipes.crafting.PatternByproductExtractionTargetCache.CachedTarget

Source: `src/main/java/logisticspipes/crafting/PatternByproductExtractionTargetCache.java:81`

- `private <init>(PatternByproductExtractionTarget target, long validUntil)` — line 86

## logisticspipes.crafting.PatternByproductPromise

Source: `src/main/java/logisticspipes/crafting/PatternByproductPromise.java:6`

- `PatternByproductTarget getByproductTarget()` — line 8

## logisticspipes.crafting.PatternByproductTarget

Source: `src/main/java/logisticspipes/crafting/PatternByproductTarget.java:10`

- `public <init>(int outputSlot, int satelliteId, String satelliteUuid, boolean fluid)` — line 26
- `public <init>(int patternSlot, int outputSlot, int satelliteId, String satelliteUuid, boolean fluid, PatternCraftingReference sourceReference)` — line 30
- `static PatternByproductTarget readFromNBT(NBTTagCompound tag, String prefix)` — line 40
- `public int getPatternSlot()` — line 55
- `public PatternCraftingReference getSourceReference()` — line 59
- `public PatternByproductTarget withSourceReference(PatternCraftingReference reference)` — line 63
- `public int getOutputSlot()` — line 67
- `public int getSatelliteId()` — line 71
- `public String getSatelliteUuid()` — line 75
- `public boolean isFluid()` — line 79
- `public boolean isConfigured()` — line 83
- `void writeToNBT(NBTTagCompound tag, String prefix)` — line 87
- `@Override public boolean equals(Object obj)` — line 98
- `@Override public int hashCode()` — line 113

## logisticspipes.crafting.PatternCraftingArrivalHandler

Source: `src/main/java/logisticspipes/crafting/PatternCraftingArrivalHandler.java:18`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternHandler patternHandler, PatternStackBufferHandler ingredientBuffer, PatternStackRequestHandler requestedIngredient, PatternCraftingIngredientPlanner ingredientPlanner, PatternCraftingCancelHandler cancelHandler)` — line 28
- `void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 41
- `private void solidItemArrived(PatternTargetInformation target, ItemStack pattern, ItemIdentifierStack item)` — line 62
- `private void fluidArrived(PatternTargetInformation target, ItemStack pattern, ItemIdentifierStack routedStack, FluidStack fluidStack)` — line 111
- `private void sendToStorage(PatternTargetInformation target, ItemIdentifierStack item, FluidStack fluid)` — line 168

## logisticspipes.crafting.PatternCraftingBatchOutputs

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBatchOutputs.java:26`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, AdjacentInventoryHandler adjacent)` — line 39
- `boolean prepare(PatternSatelliteDispatchHandler.DispatchPlan plan)` — line 48
- `boolean canUseTargets(PatternSatelliteDispatchHandler.DispatchPlan plan)` — line 89
- `void committed(PatternSatelliteDispatchHandler.DispatchPlan plan)` — line 116
- `void discardPrepared(PatternSatelliteDispatchHandler.DispatchPlan plan)` — line 124
- `private long occupied(boolean fluid)` — line 132
- `void collect()` — line 139
- `boolean arrival(ItemIdentifierStack arrived, PatternTargetInformation info)` — line 203
- `private void returnArrivalToStorage(ItemIdentifierStack stack)` — line 244
- `int missing(PatternTargetInformation info, IPatternStack stack)` — line 253
- `boolean lost(PatternTargetInformation info, IPatternStack stack)` — line 262
- `int incoming(IPatternStack stack)` — line 276
- `IPatternStack take(LogisticsOrder order, int maxAmount)` — line 283
- `private boolean matches(Batch batch, Output output, LogisticsOrder order)` — line 297
- `boolean manages(LogisticsOrder order)` — line 310
- `void forgetJob(UUID instance)` — line 323
- `void manageJob(UUID instance)` — line 327
- `private logisticspipes.utils.tuples.Pair<Integer, Integer> storageReply(IPatternStack stack, int wanted)` — line 331
- `int storageRoom(IPatternStack stack, int wanted)` — line 349
- `logisticspipes.logisticspipes.IRoutedItem sendToStorage(IPatternStack stack)` — line 353
- `void putBack(LogisticsOrder order, IPatternStack stack)` — line 367
- `boolean hasInstance(UUID instance)` — line 376
- `boolean cancelInstance(UUID instance)` — line 381
- `List<PatternCraftingMonitorEntry> monitorEntries()` — line 397
- `boolean activePattern(int slot)` — line 415
- `String patternStatus(int slot)` — line 420
- `long unreservedAmount(java.util.Set<UUID> admitted, boolean fluid)` — line 431
- `void cleanup()` — line 441
- `void returnUnclaimedOutputs()` — line 452
- `void dropContents(net.minecraft.world.World world, int x, int y, int z)` — line 487
- `void readFromNBT(NBTTagCompound tag, PatternSatelliteDispatchHandler dispatcher)` — line 498
- `void writeToNBT(NBTTagCompound tag)` — line 532
- `void appendDebugState(StringBuilder out)` — line 564
- `static void clear()` — line 623

## logisticspipes.crafting.PatternCraftingBatchOutputs.Batch

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBatchOutputs.java:580`

- `<init>(PatternSatelliteDispatchHandler.DispatchPlan plan, PipeItemsPatternCraftingLogistics.BlockingMode mode)` — line 588
- `boolean drained()` — line 593
- `boolean empty()` — line 599

## logisticspipes.crafting.PatternCraftingBatchOutputs.Output

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBatchOutputs.java:607`

- `<init>(IPatternStack stack, PatternByproductTarget target)` — line 616

## logisticspipes.crafting.PatternCraftingBlockingHandler

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBlockingHandler.java:22`

- `<init>(ModulePatternCrafting module)` — line 31
- `void readFromNBT(NBTTagCompound tag, PatternSatelliteDispatchHandler dispatcher)` — line 35
- `void writeToNBT(NBTTagCompound tag)` — line 45
- `int runningCraft()` — line 51
- `boolean runningCraftInAdjacent()` — line 55
- `PatternCraftingReference runningCraftReference()` — line 59
- `boolean hasSatelliteBatches()` — line 63
- `List<Integer> satelliteBatchPatternSlots()` — line 68
- `boolean hasSatelliteBatchFor(int patternSlot)` — line 79
- `boolean isPatternActive(int patternSlot)` — line 84
- `void restoreRunningCraft(int patternSlot, PatternCraftingReference reference, boolean inAdjacent)` — line 88
- `void activateFromBuffer(int patternSlot, PatternCraftingReference reference)` — line 99
- `boolean canReceiveForPattern(int patternSlot)` — line 108
- `boolean shouldSkipPushFor(int patternSlot)` — line 125
- `void markDispatched(int patternSlot, PatternCraftingReference reference, SatelliteBatch satelliteBatch, boolean usesLocalInventory)` — line 136
- `boolean isRunningCraftLocked()` — line 150
- `boolean isRunningCraftLocked(AdjacentTile connected)` — line 154
- `void refreshRunningCraftState(AdjacentTile connected)` — line 165
- `String getHudSatelliteStatus(int patternSlot)` — line 197
- `boolean isBlockedByOtherRunningCraft(int patternSlot, AdjacentTile connected)` — line 209
- `boolean retrieveAndReleaseSatelliteBatches(Collection<Integer> patternSlots)` — line 220
- `boolean retrieveAndReleaseSatelliteBatches(UUID instanceId)` — line 238
- `boolean clearRunningCraft(UUID instanceId)` — line 256
- `boolean retrieveAndReleaseAllSatelliteBatches()` — line 264
- `boolean releaseAllSatelliteBatches()` — line 276
- `private void refreshSatelliteBatches()` — line 288
- `private boolean hasActiveSatelliteBatchWithoutRefresh()` — line 311
- `private boolean hasSatelliteBatchForWithoutRefresh(int patternSlot)` — line 315
- `private void setRunningCraft(int patternSlot, PatternCraftingReference reference, boolean inAdjacent)` — line 324

## logisticspipes.crafting.PatternCraftingBlockingHandler.SatelliteBatch

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBlockingHandler.java:335`

- `PatternCraftingReference ownerReference()` — line 337
- `int patternSlot()` — line 339
- `int size()` — line 341
- `boolean isConsumed()` — line 343
- `void release()` — line 345
- `void retrieveAndRelease()` — line 347
- `NBTTagCompound writeToNBT()` — line 349

## logisticspipes.crafting.PatternCraftingBranch

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBranch.java:27`

- `void collectWorkspace(Map<ModulePatternCrafting, PatternCraftingWorkspace.Budget> plan)` — line 65
- `public <init>(IResource requestType, IAdditionalTargetInformation info, List<IPromise> promises, List<IExtraPromise> extraPromises, List<IExtraPromise> byproducts, List<PatternCraftingBranch> subRequests)` — line 91
- `private <init>(IResource requestType, IAdditionalTargetInformation info, int originalAmount, int remainingAmount, List<PromiseState> promises, List<ExtraState> extraPromises, List<ExtraState> byproducts, List<PatternCraftingBranch> subRequests)` — line 105
- `private <init>(IResource requestType, IAdditionalTargetInformation info, int originalAmount, int remainingAmount, int originalCraftingAmount, int remainingCraftingAmount, List<PromiseState> promises, List<ExtraState> extraPromises, List<ExtraState> byproducts, List<PatternCraftingBranch> subRequests)` — line 121
- `static PatternCraftingBranch readFromNBT(NBTTagCompound tag)` — line 137
- `private static List<PatternCraftingBranch> mergeCompatibleBranches(List<PatternCraftingBranch> branches)` — line 163
- `private static int findCompatibleBranch(List<PatternCraftingBranch> branches, PatternCraftingBranch candidate)` — line 176
- `private static List<PromiseState> copyPromiseStates(List<IPromise> promises)` — line 185
- `private static List<ExtraState> copyExtraStates(List<IExtraPromise> promises)` — line 196
- `private static List<PromiseState> readPromiseStates(NBTTagList list)` — line 204
- `private static List<ExtraState> readExtraStates(NBTTagList list)` — line 218
- `private static List<PatternCraftingBranch> readSubRequests(NBTTagList list)` — line 230
- `private static int countCraftingAmount(List<PromiseState> promises)` — line 241
- `private static int countCraftingSets(List<PromiseState> promises)` — line 254
- `private static int craftingSetsForAmount(IPromise promise, int amount)` — line 264
- `private static int resultAmountPerSet(IPromise promise)` — line 272
- `private static IPromise copyPromiseForAmount(IPromise promise, int amount)` — line 285
- `private static int scaleAmount(int amount, int numerator, int denominator)` — line 306
- `public List<PatternCraftingBranch> getSubRequests()` — line 321
- `IAdditionalTargetInformation getTargetInformation()` — line 328
- `void attachDebugModule(ModulePatternCrafting module)` — line 337
- `private static List<ExtraState> mergeByproductStates(List<ExtraState> states)` — line 344
- `private static PatternByproductTarget byproductTarget(IExtraPromise promise)` — line 369
- `private boolean producesByproduct(IPromise promise, IExtraPromise byproduct)` — line 373
- `private int byproductAmountForNext(ExtraState state, int craftingAmount)` — line 390
- `int getCraftingSets()` — line 412
- `List<IExtraPromise> getByproductPromises()` — line 416
- `void bindToInstance(PatternCraftingReference ownerReference)` — line 426
- `PatternCraftingReference reference()` — line 452
- `void writeGraph(Map<PatternCraftingReference, NBTTagCompound> records)` — line 457
- `static Map<PatternCraftingReference, PatternCraftingBranch> readGraph(NBTTagList records)` — line 473
- `private void writeRecord(NBTTagCompound tag)` — line 495
- `public void appendDebugState(StringBuilder out, String prefix)` — line 529
- `public boolean matches(ItemIdentifier item)` — line 549
- `public boolean matches(FluidIdentifier fluid)` — line 556
- `PatternCraftingMonitorNode toMonitorNode(Set<PatternCraftingOrder> visitedOrders)` — line 563
- `void collectNestedCraftingOrders(Set<PatternCraftingOrder> nestedOrders)` — line 591
- `private void debugBranchEvent(String category, String message, Object... args)` — line 604
- `private ModulePatternCrafting findDebugModule()` — line 611
- `public int request(int amount)` — line 635
- `public int request(int amount, IRequestItems targetOverride, IAdditionalTargetInformation infoOverride)` — line 645
- `public int request(int amount, IRequestFluid targetOverride, IAdditionalTargetInformation infoOverride)` — line 655
- `private int request(int amount, IRequestItems targetOverride, IRequestFluid fluidTargetOverride, IAdditionalTargetInformation infoOverride)` — line 659
- `private IAdditionalTargetInformation createOrderTarget(IAdditionalTargetInformation infoOverride)` — line 800
- `private IResource copyRequestForTarget(int amount, IRequestItems targetOverride, IRequestFluid fluidTargetOverride)` — line 807
- `public PatternCraftingBranch copyForAmount(int amount)` — line 838
- `private void registerExtrasFor(int craftingAmount)` — line 882
- `private void registerOverflowExtrasFor(List<ExtraState> states, int craftingAmount)` — line 894
- `private void registerByproductsFor(List<ExtraState> states, int craftingAmount)` — line 930
- `private void registerExtra(IExtraPromise promise, int craftingAmount)` — line 953
- `public PatternCraftingBranch copyAndReserve(int amount)` — line 964
- `public void reserveProviderPromises()` — line 983
- `public void releaseProviderPromises()` — line 1010
- `private void requestSubRequestsFor(int amount)` — line 1037
- `private void reserveSubRequestsFor(int amount)` — line 1059
- `public void reserve(int amount)` — line 1081
- `private int requestAmountForPromiseBatch(int startIndex, int maxAmount)` — line 1117
- `private void consumePromiseBatch(int startIndex, IPromise firstPromise, int amount)` — line 1142
- `private boolean isMergeableStagedPromise(IPromise promise)` — line 1158
- `private boolean canMergePromiseBatch(IPromise first, IPromise candidate)` — line 1162
- `private List<PromiseState> copyPromiseStatesFor(int amount)` — line 1196
- `private void consumePromises(int amount)` — line 1220
- `private List<BranchAllocation> allocateChildrenForCraftingAmount(int craftingAmount)` — line 1243
- `private int craftingAmountForNext(int amount)` — line 1286
- `private int consumedCraftingSetsForNext(int extraCraftingAmount)` — line 1305
- `private boolean canMergeWith(PatternCraftingBranch other)` — line 1322
- `private PatternCraftingBranch mergeWith(PatternCraftingBranch other)` — line 1329
- `private NBTTagList writePromiseStates()` — line 1349
- `private NBTTagList writeExtraStates(List<ExtraState> states)` — line 1366
- `private List<ExtraState> copyOverflowExtraStatesFor(List<ExtraState> states, int craftingAmount)` — line 1385
- `private List<ExtraState> copyByproductStatesFor(List<ExtraState> states, int craftingAmount)` — line 1408
- `private void appendPromises(StringBuilder out, String prefix)` — line 1424
- `private void appendLiveOrders(StringBuilder out, String prefix)` — line 1438
- `private int getLiveOrderAmount()` — line 1450
- `private boolean hasInProgressOrders()` — line 1462
- `private void resolveLiveOrders()` — line 1472
- `private void appendExtraStates(StringBuilder out, String prefix, String label, List<ExtraState> states)` — line 1483

## logisticspipes.crafting.PatternCraftingBranch.BranchAllocation

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBranch.java:1494`

- `private <init>(PatternCraftingBranch branch, int amount)` — line 1499

## logisticspipes.crafting.PatternCraftingBranch.PromiseState

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBranch.java:1505`

- `private <init>(IPromise promise, int remainingAmount, boolean providerReserved)` — line 1512

## logisticspipes.crafting.PatternCraftingBranch.ExtraState

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBranch.java:1519`

- `private <init>(IExtraPromise promise)` — line 1525
- `private <init>(IExtraPromise promise, int originalAmount)` — line 1530
- `private int amountForRange(int consumedBefore, int consumedAfter, int parentAmount)` — line 1535
- `private int scaledAmount(int consumed, int parentAmount)` — line 1539

## logisticspipes.crafting.PatternCraftingBufferDispatcher

Source: `src/main/java/logisticspipes/crafting/PatternCraftingBufferDispatcher.java:15`

- `<init>(ModulePatternCrafting module, PatternStackBufferHandler ingredientBuffer, AdjacentInventoryHandler adjacentInventory, PatternCraftingBlockingHandler blockingHandler, PatternSatelliteDispatchHandler satelliteDispatchHandler, PatternCraftingIngredientPlanner ingredientPlanner)` — line 30
- `void refreshSatelliteBatches()` — line 42
- `void readFromNBT(NBTTagCompound tag)` — line 46
- `void writeToNBT(NBTTagCompound tag)` — line 61
- `void deferCleanup(PatternSatelliteDispatchHandler.DispatchPlan plan, boolean returnToStorage)` — line 76
- `void retryDeferredCleanup()` — line 81
- `void pushBufferedIngredients()` — line 93
- `void pushBufferedIngredientsFor(int patternSlot)` — line 114
- `private void pushBufferedIngredientsFor(PatternCraftingReference ownerReference, int patternSlot)` — line 118
- `private void resumePendingDispatch()` — line 167
- `private void finishDispatch(PatternSatelliteDispatchHandler.DispatchPlan plan)` — line 187
- `int pendingDispatchSlot()` — line 220
- `boolean abandonPendingDispatch(java.util.UUID instanceId)` — line 228
- `private boolean abandonPendingDispatch()` — line 239
- `int completeBufferedSets(int patternSlot)` — line 247
- `private int completeBufferedSets(PatternCraftingReference owner, int patternSlot)` — line 254
- `PatternCraftingReference findCompleteBufferedOwner(int patternSlot)` — line 259
- `int findCompleteBufferedPattern()` — line 268
- `void refreshRunningCraftState()` — line 281

## logisticspipes.crafting.PatternCraftingCancelHandler

Source: `src/main/java/logisticspipes/crafting/PatternCraftingCancelHandler.java:22`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternHandler patternHandler, PatternStackBufferHandler ingredientBuffer, Map<Integer, List<IPatternStack>> requestedIngredients, PatternStackRequestHandler requestedIngredient, PatternStagedCraftingCoordinator stagedCrafting, PatternCraftingBlockingHandler blockingHandler, PatternLostIngredientHandler lostIngredientHandler)` — line 34
- `boolean cancelPatternCraft(int patternSlot)` — line 50
- `boolean cancelTrackedOrder(PatternCraftingOrder order)` — line 66
- `boolean cancelPendingInstance(UUID instanceId)` — line 84
- `boolean cancelStandaloneInstance(UUID instanceId)` — line 88
- `private boolean cancelUntrackedInstance(UUID instanceId, boolean removeOrders)` — line 92
- `private boolean removeStandaloneOrders(UUID instanceId)` — line 109
- `private boolean belongsToInstance(LogisticsOrder order, UUID instanceId)` — line 144
- `boolean returnStoredInputsToStorage()` — line 148
- `boolean shouldRouteLateArrivalToStorage(PatternCraftingReference reference)` — line 178
- `private boolean flushBufferedIngredientsToStorage(PatternCraftingReference owner, int patternSlot)` — line 182
- `private boolean flushBufferedIngredientsToStorage(int patternSlot)` — line 186
- `private boolean sendToStorage(List<IPatternStack> stacks, int patternSlot, PatternCraftingReference owner)` — line 190

## logisticspipes.crafting.PatternCraftingCapacity

Source: `src/main/java/logisticspipes/crafting/PatternCraftingCapacity.java:14`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternHandler patterns, PatternStackRequestHandler requested, PatternCraftingIngredientPlanner ingredients)` — line 22
- `int spaceForItem(ItemIdentifier item, boolean includeInTransit)` — line 32
- `int spaceForFluid(FluidIdentifier fluid, boolean includeInTransit)` — line 42

## logisticspipes.crafting.PatternCraftingHudHandler

Source: `src/main/java/logisticspipes/crafting/PatternCraftingHudHandler.java:22`

- `<init>(ModulePatternCrafting module, PatternHandler patternHandler, AdjacentInventoryHandler adjacentInventory, PatternStackBufferHandler ingredientBuffer, Map<Integer, List<IPatternStack>> requestedIngredients, PatternStagedCraftingCoordinator stagedCrafting, PatternCraftingBlockingHandler blockingHandler, PatternSatelliteDispatchHandler satelliteDispatchHandler)` — line 39
- `PatternCraftingHudState getHudState()` — line 53
- `boolean shouldRefreshHudState()` — line 62
- `void markDirty()` — line 66
- `private boolean isRecheckDue()` — line 70
- `private PatternCraftingHudState buildState()` — line 75
- `private PatternCraftingHudState.PatternInfo buildPatternInfo(int slot, ItemStack pattern)` — line 88
- `private String getStatus(int patternSlot, ItemStack pattern)` — line 118
- `private String getBufferedStatus(int patternSlot, ItemStack pattern, AdjacentTile connected, PipeItemsPatternCraftingLogistics.BlockingMode mode, int bufferedSets)` — line 166
- `private String getPendingIngredient(int patternSlot, ItemStack pattern)` — line 182
- `private String ingredientName(IPatternStack stack)` — line 199
- `private String formatSets(int sets)` — line 204
- `private int totalAmount(List<IPatternStack> stacks)` — line 208

## logisticspipes.crafting.PatternCraftingHudState

Source: `src/main/java/logisticspipes/crafting/PatternCraftingHudState.java:17`

- `public <init>()` — line 23
- `public <init>(PipeItemsPatternCraftingLogistics.BlockingMode blockingMode)` — line 27
- `public <init>(PipeItemsPatternCraftingLogistics.BlockingMode blockingMode, List<PatternInfo> patterns)` — line 31
- `public static PatternCraftingHudState empty()` — line 37
- `public void writeData(LPDataOutputStream data)` — line 41
- `public static PatternCraftingHudState readData(LPDataInputStream data)` — line 46
- `@Override public boolean equals(Object o)` — line 52
- `@Override public int hashCode()` — line 63

## logisticspipes.crafting.PatternCraftingHudState.PatternInfo

Source: `src/main/java/logisticspipes/crafting/PatternCraftingHudState.java:68`

- `public <init>(int slot)` — line 78
- `private <init>(int slot, List<IngredientInfo> ingredients, List<OutputInfo> outputs, String status, boolean active)` — line 82
- `public void setStatus(String status)` — line 91
- `private void writeData(LPDataOutputStream data)` — line 95
- `private static PatternInfo readData(LPDataInputStream data)` — line 103
- `@Override public boolean equals(Object o)` — line 112
- `@Override public int hashCode()` — line 126

## logisticspipes.crafting.PatternCraftingHudState.OutputInfo

Source: `src/main/java/logisticspipes/crafting/PatternCraftingHudState.java:132`

- `public <init>(ItemIdentifierStack stack, int requestedAmount, int slot)` — line 135
- `private void writeData(LPDataOutputStream data)` — line 141
- `private static OutputInfo readData(LPDataInputStream data)` — line 147
- `@Override public boolean equals(Object o)` — line 151

## logisticspipes.crafting.PatternCraftingHudState.IngredientInfo

Source: `src/main/java/logisticspipes/crafting/PatternCraftingHudState.java:164`

- `public <init>(ItemIdentifierStack stack, int bufferedAmount, int slot)` — line 167
- `private void writeData(LPDataOutputStream data)` — line 173
- `private static IngredientInfo readData(LPDataInputStream data)` — line 179
- `@Override public boolean equals(Object o)` — line 183

## logisticspipes.crafting.PatternCraftingIngredientPlanner

Source: `src/main/java/logisticspipes/crafting/PatternCraftingIngredientPlanner.java:30`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternHandler patternHandler, AdjacentInventoryHandler adjacentInventory, PatternStackBufferHandler ingredientBuffer, PatternStackRequestHandler requestedIngredient)` — line 42
- `void invalidate()` — line 53
- `List<PatternIngredientTarget> getIngredientTargets(ItemStack pattern)` — line 59
- `List<IPatternStack> getAggregatedIngredients(ItemStack pattern)` — line 64
- `List<IPatternStack> getLocalAggregatedIngredients(ItemStack pattern)` — line 69
- `IRequestItems getSatelliteTargetForInputSlot(ItemStack pattern, int inputSlot)` — line 74
- `IRequestFluid getFluidSatelliteTargetForInputSlot(ItemStack pattern, int inputSlot)` — line 83
- `boolean hasLinkedSatelliteAssignment(ItemStack pattern, int inputSlot)` — line 92
- `boolean hasLinkedSatelliteAssignments(ItemStack pattern)` — line 101
- `int ingredientAmount(ItemStack pattern, ItemIdentifier item)` — line 106
- `private boolean ingredientMatchesItem(ItemStack pattern, IPatternStack ingredient, ItemIdentifier item)` — line 125
- `int bufferedIngredientAmount(int patternSlot, ItemStack pattern, IPatternStack ingredient)` — line 131
- `int bufferedItemAmount(int patternSlot, ItemStack pattern, ItemIdentifier item)` — line 145
- `int requestedIngredientAmount(int patternSlot, ItemStack pattern, IPatternStack ingredient)` — line 163
- `int requestedItemAmount(int patternSlot, ItemStack pattern, ItemIdentifier item)` — line 168
- `int requestedItemAmount(PatternCraftingReference owner, ItemStack pattern, ItemIdentifier item)` — line 178
- `int removeRequestedItem(PatternCraftingReference owner, int patternSlot, ItemStack pattern, ItemIdentifier item, int amount)` — line 188
- `boolean requiresConcreteIngredientPlanning(ItemStack pattern)` — line 199
- `List<PatternIngredientAssignment> buildBufferedIngredientPlan(int patternSlot, ItemStack pattern, int sets)` — line 204
- `List<PatternIngredientAssignment> buildBufferedIngredientPlan(PatternCraftingReference owner, int patternSlot, ItemStack pattern, int sets)` — line 208
- `List<PatternIngredientAssignment> buildBufferedIngredientPlanAfterAdding(int patternSlot, ItemStack pattern, int sets, IPatternStack arrivingStack)` — line 213
- `int completeBufferedSets(int patternSlot, ItemStack pattern)` — line 218
- `int completeBufferedSets(PatternCraftingReference owner, int patternSlot, ItemStack pattern)` — line 226
- `private int matchingAmount(ItemStack pattern, List<IPatternStack> stacks, IPatternStack ingredient)` — line 254
- `private TargetPlan getTargetPlan(ItemStack pattern)` — line 264
- `private TargetPlan buildTargetPlan(ItemStack pattern)` — line 278
- `private void refreshTargetPlanTick()` — line 312
- `private boolean ingredientMatchesStack(ItemStack pattern, IPatternStack ingredient, IPatternStack buffered)` — line 322
- `private boolean itemMatchesPatternIngredient(PatternRecipeSnapshot recipe, ItemIdentifier expected, ItemIdentifier actual)` — line 331
- `private List<PatternIngredientAssignment> buildBufferedIngredientPlan(int patternSlot, ItemStack pattern, List<PatternIngredientTarget> ingredients, int sets, IPatternStack extraStack)` — line 348
- `private List<PatternIngredientAssignment> buildBufferedIngredientPlan(int patternSlot, ItemStack pattern, List<PatternIngredientTarget> ingredients, int sets, IPatternStack extraStack, PatternCraftingReference owner)` — line 353
- `private List<IPatternStack> copyBufferedIngredients(int patternSlot)` — line 379
- `private IPatternStack takeMatchingStack(ItemStack pattern, List<IPatternStack> available, IPatternStack ingredient, int amount)` — line 393

## logisticspipes.crafting.PatternCraftingIngredientPlanner.TargetPlan

Source: `src/main/java/logisticspipes/crafting/PatternCraftingIngredientPlanner.java:410`

- `private <init>(List<PatternIngredientTarget> ingredients, List<PatternIngredientTarget> localIngredients, boolean hasSatelliteAssignments)` — line 418
- `private static List<IPatternStack> aggregate(List<PatternIngredientTarget> targets)` — line 426

## logisticspipes.crafting.PatternCraftingIngredientPlanner.CompleteSetsCache

Source: `src/main/java/logisticspipes/crafting/PatternCraftingIngredientPlanner.java:435`

- `private <init>(ItemStack pattern, long bufferVersion, int sets)` — line 441

## logisticspipes.crafting.PatternCraftingInstanceRegistry

Source: `src/main/java/logisticspipes/crafting/PatternCraftingInstanceRegistry.java:24`

- `private <init>()` — line 31
- `static synchronized void register(IOrderInfoProvider outputOrder, PatternCraftingOrder order)` — line 33
- `static synchronized PatternCraftingOrder find(IOrderInfoProvider outputOrder)` — line 45
- `static synchronized boolean isTrackedOutputOrder(IOrderInfoProvider outputOrder)` — line 62
- `static synchronized PatternCraftingOrder find(PatternCraftingReference reference)` — line 66
- `static synchronized List<PatternCraftingOrder> ordersForInstance(UUID instanceId)` — line 70
- `static boolean cancelInstance(UUID instanceId)` — line 85
- `static synchronized boolean isCancelled(PatternCraftingReference reference)` — line 98
- `static synchronized void recordCancellation(UUID instanceId)` — line 102
- `static synchronized void unregister(PatternCraftingOrder order)` — line 108
- `private static void markCancelled(UUID instanceId)` — line 123
- `static synchronized List<PatternCraftingOrder> liveOrders()` — line 131
- `public static synchronized void clear()` — line 135

## logisticspipes.crafting.PatternCraftingMonitorEntry

Source: `src/main/java/logisticspipes/crafting/PatternCraftingMonitorEntry.java:14`

- `public <init>(UUID instanceId, List<PatternCraftingMonitorNode> roots)` — line 22
- `private <init>(UUID instanceId, List<PatternCraftingMonitorNode> roots, boolean restoring, int restoreAttempts, int maxRestoreAttempts)` — line 26
- `public static PatternCraftingMonitorEntry restoring(UUID instanceId, List<PatternCraftingMonitorNode> roots, int restoreAttempts, int maxRestoreAttempts)` — line 35
- `public static PatternCraftingMonitorEntry readData(LPDataInputStream data)` — line 40
- `public UUID getInstanceId()` — line 53
- `public List<PatternCraftingMonitorNode> getRoots()` — line 57
- `public ItemIdentifierStack getDisplayStack()` — line 61
- `public boolean isInProgress()` — line 70
- `public boolean isRestoring()` — line 79
- `public int getRestoreAttempts()` — line 83
- `public int getMaxRestoreAttempts()` — line 87
- `public void writeData(LPDataOutputStream data)` — line 91

## logisticspipes.crafting.PatternCraftingMonitorNode

Source: `src/main/java/logisticspipes/crafting/PatternCraftingMonitorNode.java:13`

- `public <init>(ItemIdentifierStack stack, int unrequestedAmount, int orderedAmount, boolean inProgress)` — line 25
- `public List<PatternCraftingMonitorNode> getChildren()` — line 33
- `public void addChild(PatternCraftingMonitorNode child)` — line 37
- `public void addChildren(List<PatternCraftingMonitorNode> nodes)` — line 44
- `public boolean hasVisibleWork()` — line 50
- `public int getTreeRootSize()` — line 54
- `public int getDepth()` — line 62
- `public void writeData(LPDataOutputStream data)` — line 70
- `public static PatternCraftingMonitorNode readData(LPDataInputStream data)` — line 81

## logisticspipes.crafting.PatternCraftingMonitorRegistry

Source: `src/main/java/logisticspipes/crafting/PatternCraftingMonitorRegistry.java:20`

- `private <init>()` — line 22
- `static PatternCraftingOrder find(IOrderInfoProvider outputOrder)` — line 24
- `public static void clear()` — line 28
- `public static List<PatternCraftingMonitorNode> build(LinkedLogisticsOrderList orders)` — line 32
- `public static List<PatternCraftingMonitorEntry> buildAll(IRouter networkRouter)` — line 45
- `public static boolean cancelInstance(UUID instanceId, IRouter networkRouter)` — line 104
- `private static void mergeEntry(Map<UUID, PatternCraftingMonitorEntry> entriesByInstance, PatternCraftingMonitorEntry addition)` — line 137
- `private static void appendMonitorNodes(LinkedLogisticsOrderList orders, List<PatternCraftingMonitorNode> result)` — line 160
- `private static void cleanupFinishedOrders()` — line 176
- `private static boolean belongsToNetwork(PatternCraftingOrder order, IRouter networkRouter)` — line 186
- `static List<ModulePatternCrafting> networkPatternModules(IRouter networkRouter)` — line 193
- `private static void appendPatternModule(IRouter router, Set<LogisticsModule> seen, List<ModulePatternCrafting> result)` — line 204
- `private static String displayName(PatternCraftingMonitorEntry entry)` — line 215

## logisticspipes.crafting.PatternCraftingOrder

Source: `src/main/java/logisticspipes/crafting/PatternCraftingOrder.java:23`

- `<init>(PatternCraftingReference reference, int patternSlot, int resultAmountPerSet, PatternCraftingBranch branch, IOrderInfoProvider outputOrder, ModulePatternCrafting module, PatternStackRequestHandler requestedIngredient)` — line 55
- `<init>(PatternCraftingReference reference, int patternSlot, int resultAmountPerSet, int remainingSets, PatternCraftingBranch rootBranch, List<PatternCraftingBranch> ingredientBranches, IOrderInfoProvider outputOrder, ModulePatternCrafting module, PatternStackRequestHandler requestedIngredient)` — line 93
- `void ingredientsDispatched(int sets)` — line 125
- `int extractableOutputAmount()` — line 145
- `boolean usesBatchExecution()` — line 154
- `boolean isFullyRequested()` — line 164
- `boolean isFullyDispatched()` — line 169
- `private int initialRemainingSets(PatternCraftingBranch branch)` — line 188
- `private int capRemainingSets(int sets)` — line 194
- `int availableSetsFromBranches(ItemStack pattern)` — line 216
- `int requestIngredients(ItemStack pattern, int sets)` — line 229
- `void releaseReservations()` — line 291
- `PatternCraftingReference reference()` — line 304
- `ModulePatternCrafting module()` — line 308
- `void writeRuntimeState(NBTTagCompound tag)` — line 315
- `void readRuntimeState(NBTTagCompound tag)` — line 349
- `void appendDebugState(StringBuilder out, String prefix)` — line 376
- `PatternCraftingMonitorNode toMonitorNode(Set<PatternCraftingOrder> visitedOrders)` — line 395
- `void collectNestedCraftingOrders(Set<PatternCraftingOrder> nestedOrders)` — line 410
- `private int availableFromBranches(PatternIngredientTarget ingredient)` — line 419
- `private int preRequestedAmount(int inputSlot)` — line 429
- `private void addPreRequestedIngredient(int inputSlot, int amount)` — line 433
- `private void commitPreRequested(ItemStack pattern, int sets)` — line 440
- `private BranchRequest requestFromBranches(IPatternStack ingredient, int amount, int inputSlot, IRequestItems itemTargetOverride, IRequestFluid fluidTargetOverride)` — line 458
- `private boolean branchMatches(PatternCraftingBranch branch, PatternIngredientTarget ingredient)` — line 509
- `private boolean branchMatches(PatternCraftingBranch branch, IPatternStack ingredient, int inputSlot)` — line 513
- `private boolean branchTargetsInputSlot(PatternCraftingBranch branch, int inputSlot)` — line 525

## logisticspipes.crafting.PatternCraftingOrder.BranchRequest

Source: `src/main/java/logisticspipes/crafting/PatternCraftingOrder.java:532`

- `private <init>(int amount)` — line 536
- `private static BranchRequest empty()` — line 540

## logisticspipes.crafting.PatternCraftingOrder.RequestedIngredient

Source: `src/main/java/logisticspipes/crafting/PatternCraftingOrder.java:545`

- `private <init>(PatternIngredientTarget ingredient, int amount)` — line 550

## logisticspipes.crafting.PatternCraftingPersistence

Source: `src/main/java/logisticspipes/crafting/PatternCraftingPersistence.java:39`

- `private <init>()` — line 84
- `static boolean writeResource(NBTTagCompound tag, IResource resource)` — line 86
- `static IResource readResource(NBTTagCompound tag)` — line 108
- `static boolean writePromise(NBTTagCompound tag, IPromise promise)` — line 130
- `static IPromise readPromise(NBTTagCompound tag)` — line 210
- `static IExtraPromise readExtraPromise(NBTTagCompound tag)` — line 299
- `static boolean writeOrder(NBTTagCompound tag, IOrderInfoProvider order)` — line 307
- `static RestoredOrder readOrder(NBTTagCompound tag)` — line 333
- `static PatternCraftingReference readOrderCraftingReference(NBTTagCompound tag)` — line 360
- `static void writeOrderCraftingReference(NBTTagCompound tag, PatternCraftingReference reference)` — line 364
- `static ItemIdentifierStack readOrderDisplayStack(NBTTagCompound tag)` — line 370
- `static void writeTargetInfo(NBTTagCompound parent, IAdditionalTargetInformation info)` — line 383
- `static IAdditionalTargetInformation readTargetInfo(NBTTagCompound tag)` — line 401
- `static IAdditionalTargetInformation readTargetInfoFromParent(NBTTagCompound parent)` — line 414
- `private static void writeOrderRuntimeState(NBTTagCompound tag, IOrderInfoProvider order)` — line 418
- `private static void restoreOrderRuntimeState(IOrderInfoProvider order, RestoredOrder state)` — line 435
- `private static void writeByproductTarget(NBTTagCompound tag, PatternByproductTarget target)` — line 450
- `private static void writeDictResource(NBTTagCompound tag, DictResource resource)` — line 456
- `private static DictResource readDictResource(NBTTagCompound tag, IRequestItems target)` — line 467
- `private static boolean writeStack(NBTTagCompound tag, ItemIdentifierStack stack)` — line 478
- `private static ItemIdentifierStack readRequiredStack(NBTTagCompound tag)` — line 488
- `private static ItemIdentifierStack readStack(NBTTagCompound tag)` — line 496
- `private static void writeFluid(NBTTagCompound tag, FluidIdentifier fluid, int amount)` — line 500
- `private static FluidIdentifier readRequiredFluid(NBTTagCompound tag)` — line 511
- `private static FluidIdentifier readFluid(NBTTagCompound tag)` — line 519
- `private static void writeResourceType(NBTTagCompound tag, ResourceType type)` — line 527
- `private static ResourceType readResourceType(NBTTagCompound tag)` — line 533
- `private static void writeItemProvider(NBTTagCompound tag, String prefix, IProvideItems provider)` — line 544
- `private static IProvideItems readItemProvider(NBTTagCompound tag, String prefix)` — line 550
- `private static void writeFluidProvider(NBTTagCompound tag, String prefix, IProvideFluids provider)` — line 559
- `private static IProvideFluids readFluidProvider(NBTTagCompound tag, String prefix)` — line 565
- `private static void writeItemRequester(NBTTagCompound tag, String prefix, IRequestItems requester)` — line 574
- `private static IRequestItems readItemRequester(NBTTagCompound tag, String prefix)` — line 580
- `private static void writeFluidRequester(NBTTagCompound tag, String prefix, IRequestFluid requester)` — line 592
- `private static IRequestFluid readFluidRequester(NBTTagCompound tag, String prefix)` — line 598
- `private static void writeRouter(NBTTagCompound tag, String prefix, IRouter router, Object routedObject)` — line 610
- `private static IRouter readOptionalRouter(NBTTagCompound tag, String prefix)` — line 618
- `private static IRouter readRequiredRouter(NBTTagCompound tag, String prefix)` — line 629
- `private static IRouter readRouter(NBTTagCompound tag, String prefix)` — line 637
- `private static Object resolveRoutedObject(IRouter router, boolean preferModule, Class<?> type)` — line 650

## logisticspipes.crafting.PatternCraftingPersistence.RestoredOrder

Source: `src/main/java/logisticspipes/crafting/PatternCraftingPersistence.java:665`

- `PatternCraftingReference craftingReference()` — line 682
- `IOrderInfoProvider create(PipeItemsPatternCraftingLogistics pipe, ModulePatternCrafting module)` — line 686

## logisticspipes.crafting.PatternCraftingPersistence.RestoreNotReadyException

Source: `src/main/java/logisticspipes/crafting/PatternCraftingPersistence.java:713`

No explicit method declarations.

## logisticspipes.crafting.PatternCraftingPromise

Source: `src/main/java/logisticspipes/crafting/PatternCraftingPromise.java:9`

- `public void setByproductTarget(PatternByproductTarget target)` — line 16
- `@Override public PatternItemByproductPromise split(int more)` — line 21
- `public <init>(ItemIdentifier item, int numberOfItems, IProvideItems sender, int patternSlot, int resultAmountPerSet)` — line 27
- `@Override public PatternCraftingPromise copy()` — line 34
- `public PatternCraftingPromise copyWithAmount(int amount)` — line 42

## logisticspipes.crafting.PatternCraftingReference

Source: `src/main/java/logisticspipes/crafting/PatternCraftingReference.java:17`

- `private <init>(UUID instanceId, UUID objectId)` — line 27
- `private <init>(UUID instanceId, UUID objectId, UUID parentId)` — line 31
- `public static PatternCraftingReference createInstance()` — line 37
- `public static PatternCraftingReference createObject(UUID instanceId)` — line 42
- `public static PatternCraftingReference readFromNBT(NBTTagCompound tag, String prefix)` — line 46
- `public PatternCraftingReference createChild()` — line 63
- `public UUID parentId()` — line 67
- `public PatternCraftingReference parent()` — line 71
- `public UUID instanceId()` — line 75
- `public UUID objectId()` — line 79
- `public boolean belongsTo(PatternCraftingReference other)` — line 83
- `public void writeToNBT(NBTTagCompound tag, String prefix)` — line 87
- `@Override public boolean equals(Object object)` — line 97
- `@Override public int hashCode()` — line 108
- `@Override public String toString()` — line 113

## logisticspipes.crafting.PatternCraftingResultExtractor

Source: `src/main/java/logisticspipes/crafting/PatternCraftingResultExtractor.java:35`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, AdjacentInventoryHandler adjacentInventory)` — line 48
- `void tick()` — line 59
- `private void extractItemsFromAdjacentInventory()` — line 70
- `private int maxExtractableItemAmount(LogisticsItemOrder order, int itemsLeft)` — line 209
- `private void sendExtracted(LogisticsItemOrder order, ItemStack extracted, ForgeDirection orientation)` — line 237
- `private void sendExtractedToLocalBuffer(LogisticsItemOrder order, ItemStack extracted)` — line 283
- `private void extractFluidsFromAdjacentHandlers()` — line 319
- `private int maxExtractableFluidAmount(LogisticsFluidOrder order)` — line 433
- `private void sendExtractedFluid(LogisticsFluidOrder order, FluidStack drained, ForgeDirection orientation)` — line 455
- `private void sendExtractedFluidToLocalBuffer(LogisticsFluidOrder order, FluidStack drained)` — line 498
- `private PatternByproductTarget remoteByproductTarget(LogisticsOrder order, boolean fluid)` — line 531
- `private int remoteDestination(LogisticsOrder order)` — line 536

## logisticspipes.crafting.PatternCraftingTargetSelector

Source: `src/main/java/logisticspipes/crafting/PatternCraftingTargetSelector.java:29`

- `public <init>(PipeItemsPatternCraftingLogistics pipe)` — line 40
- `public AdjacentTile getConnectedInventoryTile()` — line 47
- `public void clearCache()` — line 58
- `public boolean isSelectedInventory(TileEntity tile, ForgeDirection direction)` — line 65
- `public void cycleConnectedInventory(EntityPlayer player)` — line 75
- `public void writeToNBT(NBTTagCompound tag)` — line 103
- `public void readFromNBT(NBTTagCompound tag)` — line 110
- `public void writeData(LPDataOutputStream data)` — line 120
- `public void readData(LPDataInputStream data)` — line 127
- `private boolean isCachedConnectedInventoryValid()` — line 135
- `private AdjacentTile resolveConnectedInventoryTile()` — line 146
- `private List<AdjacentTile> getSelectableAdjacentInventories()` — line 166
- `private AdjacentTile getSelectableAdjacentInventory(ForgeDirection direction)` — line 180
- `private TileEntity getAdjacentTile(ForgeDirection direction)` — line 194
- `private ForgeDirection getDirectionTo(TileEntity tile)` — line 204
- `private boolean isSelectableInventory(TileEntity tile, ForgeDirection direction)` — line 221
- `private ForgeDirection directionFromOrdinal(int ordinal)` — line 234

## logisticspipes.crafting.PatternCraftingTemplate

Source: `src/main/java/logisticspipes/crafting/PatternCraftingTemplate.java:18`

- `public void setOutputTarget(PatternByproductTarget target)` — line 27
- `public int getPatternSlot()` — line 31
- `public <init>(ItemIdentifierStack result, ICraftItems crafter, int priority, int patternSlot)` — line 35
- `public <init>(ItemIdentifierStack result, ICraftItems crafter, int priority, int patternSlot, int ingredientSlots)` — line 39
- `public void addByproduct(ItemIdentifierStack stack)` — line 53
- `public void addByproduct(ItemIdentifierStack stack, PatternByproductTarget target)` — line 57
- `public void addFluidByproduct(FluidIdentifierStack stack)` — line 70
- `public void addFluidByproduct(FluidIdentifierStack stack, PatternByproductTarget target)` — line 74
- `@Override public List<IExtraPromise> getByproducts(int workSets)` — line 88
- `@Override public IPromise generatePromise(int nCraftingSetsNeeded)` — line 120
- `@Override public ICraftItems getCrafter()` — line 132
- `@Override public boolean canCraft(IResource requestType)` — line 137
- `@Override public IResource getResultResource()` — line 142
- `@Override public ItemIdentifierStack getResultStack()` — line 147

## logisticspipes.crafting.PatternCraftingTemplate.ItemByproduct

Source: `src/main/java/logisticspipes/crafting/PatternCraftingTemplate.java:152`

- `private <init>(ItemIdentifierStack stack, PatternByproductTarget target)` — line 157

## logisticspipes.crafting.PatternCraftingTemplate.FluidByproduct

Source: `src/main/java/logisticspipes/crafting/PatternCraftingTemplate.java:163`

- `private <init>(FluidIdentifierStack stack, PatternByproductTarget target)` — line 168

## logisticspipes.crafting.PatternCraftingTemplateBuilder

Source: `src/main/java/logisticspipes/crafting/PatternCraftingTemplateBuilder.java:26`

- `<init>(ModulePatternCrafting module, PatternHandler patternHandler)` — line 34
- `ICraftingTemplate addCrafting(IResource toCraft)` — line 42
- `private ICraftingTemplate buildItemTemplate(IResource toCraft, int slot, PatternRecipeSnapshot recipe)` — line 71
- `private ICraftingTemplate buildFluidTemplate(IResource toCraft, int slot, PatternRecipeSnapshot recipe)` — line 99
- `private void addItemResultByproducts(PatternCraftingTemplate template, PatternRecipeSnapshot recipe, int resultOutputSlot)` — line 128
- `private void addFluidResultByproducts(PatternFluidCraftingTemplate template, PatternRecipeSnapshot recipe, int resultOutputSlot)` — line 153
- `private PatternByproductTarget itemByproductTarget(PatternRecipeSnapshot recipe, int patternSlot, int outputSlot)` — line 175
- `private PatternByproductTarget fluidByproductTarget(PatternRecipeSnapshot recipe, int patternSlot, int outputSlot)` — line 184
- `private PatternByproductTarget byproductTarget(int patternSlot, int outputSlot, int satelliteId, String satelliteUuid, boolean fluid)` — line 193
- `private void addPatternIngredients(BaseCraftingTemplate template, PatternRecipeSnapshot recipe, int slot)` — line 201
- `private IResource createItemIngredientResource(ItemIdentifierStack item, AbstractPattern pattern)` — line 230

## logisticspipes.crafting.PatternCraftingUpgradeCache

Source: `src/main/java/logisticspipes/crafting/PatternCraftingUpgradeCache.java:6`

- `<init>(ModulePatternCrafting module)` — line 14
- `boolean supportsFluidCrafting()` — line 18
- `boolean hasAdvancedSatellite()` — line 23
- `boolean hasInstantSatellite()` — line 28
- `private void refresh()` — line 33

## logisticspipes.crafting.PatternCraftingWorkspace

Source: `src/main/java/logisticspipes/crafting/PatternCraftingWorkspace.java:19`

- `<init>(ModulePatternCrafting module)` — line 24
- `boolean admit(PatternCraftingOrder order)` — line 28
- `static void addRecipe(Map<ModulePatternCrafting, Budget> plan, ModulePatternCrafting provider, int slot, int sets)` — line 78
- `private Budget occupied()` — line 86
- `void cleanup()` — line 96
- `void readFromNBT(NBTTagCompound tag)` — line 110
- `void writeToNBT(NBTTagCompound tag)` — line 125
- `void appendDebugState(StringBuilder out)` — line 140
- `boolean queued(UUID instance)` — line 145

## logisticspipes.crafting.PatternCraftingWorkspace.Budget

Source: `src/main/java/logisticspipes/crafting/PatternCraftingWorkspace.java:149`

- `void add(IPatternStack stack, int sets, boolean output)` — line 156
- `Budget plus(Budget other)` — line 168
- `boolean fits()` — line 177
- `@Override public String toString()` — line 184

## logisticspipes.crafting.PatternFluidByproductPromise

Source: `src/main/java/logisticspipes/crafting/PatternFluidByproductPromise.java:9`

- `public <init>(FluidIdentifier fluid, int amount, IProvideFluids sender, boolean provided, PatternByproductTarget byproductTarget)` — line 13
- `@Override public PatternByproductTarget getByproductTarget()` — line 19
- `@Override public PatternFluidByproductPromise copy()` — line 24
- `@Override public PatternFluidByproductPromise copyWithAmount(int amount)` — line 29
- `@Override public IExtraPromise split(int more)` — line 34

## logisticspipes.crafting.PatternFluidCraftingPromise

Source: `src/main/java/logisticspipes/crafting/PatternFluidCraftingPromise.java:9`

- `public void setByproductTarget(PatternByproductTarget target)` — line 16
- `@Override public PatternFluidByproductPromise split(int more)` — line 21
- `public <init>(FluidIdentifier fluid, int amount, IProvideFluids sender, int patternSlot, int resultAmountPerSet)` — line 27
- `@Override public PatternFluidCraftingPromise copy()` — line 37
- `@Override public PatternFluidCraftingPromise copyWithAmount(int amount)` — line 48

## logisticspipes.crafting.PatternFluidCraftingTemplate

Source: `src/main/java/logisticspipes/crafting/PatternFluidCraftingTemplate.java:14`

- `public void setOutputTarget(PatternByproductTarget target)` — line 21
- `public int getPatternSlot()` — line 25
- `public <init>(FluidResource result, ICraftFluids crafter, int priority, int patternSlot)` — line 32
- `@Override public PatternFluidCraftingPromise generatePromise(int nResultSets)` — line 45
- `@Override public void addByproduct(ItemIdentifierStack stack)` — line 57
- `public void addByproduct(ItemIdentifierStack stack, PatternByproductTarget target)` — line 62
- `@Override public void addFluidByproduct(FluidIdentifierStack stack)` — line 68
- `public void addFluidByproduct(FluidIdentifierStack stack, PatternByproductTarget target)` — line 73
- `@Override public List<IExtraPromise> getByproducts(int workSets)` — line 82

## logisticspipes.crafting.PatternFluidCraftingTemplate.ItemByproduct

Source: `src/main/java/logisticspipes/crafting/PatternFluidCraftingTemplate.java:108`

- `private <init>(ItemIdentifierStack stack, PatternByproductTarget target)` — line 113

## logisticspipes.crafting.PatternFluidCraftingTemplate.FluidByproduct

Source: `src/main/java/logisticspipes/crafting/PatternFluidCraftingTemplate.java:119`

- `private <init>(FluidIdentifierStack stack, PatternByproductTarget target)` — line 124

## logisticspipes.crafting.PatternIngredientAssignment

Source: `src/main/java/logisticspipes/crafting/PatternIngredientAssignment.java:12`

No explicit method declarations.

## logisticspipes.crafting.PatternIngredientTarget

Source: `src/main/java/logisticspipes/crafting/PatternIngredientTarget.java:16`

- `boolean isLocal()` — line 20
- `boolean hasSatelliteTarget()` — line 24

## logisticspipes.crafting.PatternItemByproductPromise

Source: `src/main/java/logisticspipes/crafting/PatternItemByproductPromise.java:9`

- `public <init>(ItemIdentifier item, int amount, IProvideItems sender, boolean provided, PatternByproductTarget byproductTarget)` — line 13
- `@Override public PatternByproductTarget getByproductTarget()` — line 19
- `@Override public PatternItemByproductPromise copy()` — line 24
- `@Override public IExtraPromise split(int more)` — line 29

## logisticspipes.crafting.PatternLogisticsCraftingTableTileEntity

Source: `src/main/java/logisticspipes/crafting/PatternLogisticsCraftingTableTileEntity.java:36`

- `public <init>()` — line 64
- `public static boolean isSpeedUpgrade(ItemStack stack)` — line 70
- `public void onBlockBreak()` — line 75
- `@Override public void updateEntity()` — line 82
- `public void scheduleInventoryCheck()` — line 96
- `@Override public int getSizeInventory()` — line 100
- `@Override public ItemStack getStackInSlot(int slot)` — line 105
- `@Override public ItemStack decrStackSize(int slot, int count)` — line 114
- `@Override public ItemStack getStackInSlotOnClosing(int slot)` — line 126
- `@Override public void setInventorySlotContents(int slot, ItemStack stack)` — line 131
- `@Override public String getInventoryName()` — line 136
- `@Override public boolean hasCustomInventoryName()` — line 141
- `@Override public int getInventoryStackLimit()` — line 146
- `@Override public boolean isUseableByPlayer(EntityPlayer player)` — line 151
- `@Override public void openInventory()` — line 156
- `@Override public void closeInventory()` — line 159
- `@Override public boolean isItemValidForSlot(int slot, ItemStack stack)` — line 162
- `@Override public void readFromNBT(NBTTagCompound tag)` — line 168
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 184
- `private void writeCraftingPayload(NBTTagCompound tag)` — line 190
- `@Override @SideOnly(Side.CLIENT) public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel)` — line 200
- `@Override public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings)` — line 206
- `public SimpleStackInventory getInputInventory()` — line 211
- `public SimpleStackInventory getOutputInventory()` — line 216
- `public SimpleStackInventory getUpgradeInventory()` — line 221
- `public boolean canPlayerInsertInput(ItemStack stack)` — line 225
- `public boolean isIdle()` — line 229
- `public void onPlayerInventoryChanged()` — line 234
- `@Override public void InventoryChanged(IInventory inventory)` — line 245
- `public int roomForPatternPipeItem(ItemIdentifier item)` — line 252
- `public int roomForPatternPipeSlot(int slot, ItemStack stack)` — line 264
- `public boolean canPatternPipeInsertIntoSlot(int slot, ItemStack stack)` — line 283
- `public int insertFromPatternPipe(int slot, ItemStack stack)` — line 290
- `public int insertFromPatternPipe(ItemStack stack)` — line 315
- `public int[] insertPatternPlanFromPatternPipe(List<PatternIngredientAssignment> assignments)` — line 334
- `public boolean insertPatternFromPatternPipe(ItemStack pattern, int sets)` — line 360
- `public ItemStack extractOutput(IResource wanted, int count)` — line 395
- `public double getProgress()` — line 412
- `public int getSpeedUpgradeCount()` — line 420
- `private void tryStartCrafting()` — line 431
- `private int getReducedCooldown()` — line 447
- `private void finishCraftIfReady()` — line 451
- `private boolean consumeCraftableInputsToPendingOutput()` — line 465
- `private AutoCraftingInventory createPreviewInventory()` — line 484
- `private AutoCraftingInventory createSingleItemCraftingInventory()` — line 497
- `private void consumeCraftingInputs(AutoCraftingInventory craftingInventory)` — line 510
- `private IRecipe findRecipe(AutoCraftingInventory inventory)` — line 518
- `private boolean canFitPendingOutput(ItemStack result)` — line 527
- `private boolean canFitOutput(ItemStack result)` — line 531
- `private boolean canFitInventory(SimpleStackInventory inventory, ItemStack result)` — line 535
- `private boolean addToInventory(SimpleStackInventory inventory, ItemStack stack)` — line 556
- `private void finishPendingOutputIfPossible()` — line 582
- `private boolean canAcceptInput()` — line 604
- `private boolean hasPendingOutput()` — line 608
- `private void moveRemainingInputs(AutoCraftingInventory craftingInventory)` — line 617
- `public boolean isInputEmpty()` — line 636
- `private boolean hasOutput()` — line 645
- `private boolean isCraftCoolingDown()` — line 654
- `private boolean isClientSide()` — line 658
- `private void clearInventory(SimpleStackInventory inventory)` — line 662

## logisticspipes.crafting.PatternLostIngredientHandler

Source: `src/main/java/logisticspipes/crafting/PatternLostIngredientHandler.java:25`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternStackRequestHandler requestedIngredient)` — line 37
- `int size()` — line 44
- `void queue(IPatternStack stack, IAdditionalTargetInformation info, long delay)` — line 48
- `boolean clear()` — line 58
- `boolean removeInstance(UUID instanceId)` — line 67
- `void fluidSendFailed(FluidIdentifier fluid, Integer amount)` — line 77
- `void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 85
- `void retryLostItems()` — line 100
- `void readFromNBT(NBTTagCompound tag)` — line 120
- `void writeToNBT(NBTTagCompound tag)` — line 135
- `private int requestLostIngredient(IPatternStack stack, PatternTargetInformation target)` — line 152

## logisticspipes.crafting.PatternSatelliteByproductExtractor

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteByproductExtractor.java:42`

- `<init>(CoreRoutedPipe satellite)` — line 52
- `private static int fluidEnergy(int amount)` — line 56
- `boolean canExtractFor(IRouter requester)` — line 60
- `PatternByproductExtractionResult extractItem(ItemIdentifier item, int amount, int destination, IAdditionalTargetInformation info)` — line 75
- `PatternByproductExtractionResult extractFluid(FluidIdentifier fluid, int amount, int destination, IAdditionalTargetInformation info)` — line 103
- `private List<AdjacentTile> getItemTargets()` — line 156
- `private List<AdjacentTile> getFluidTargets()` — line 165
- `private List<AdjacentTile> findTargets(boolean items)` — line 174
- `private boolean containsInvalidTile(List<AdjacentTile> targets)` — line 201
- `private IInventory extractionInventory(AdjacentTile target)` — line 210
- `private IRoutedItem queueItem(ItemStack stack, ForgeDirection from, int destination, IAdditionalTargetInformation info)` — line 223
- `private IRoutedItem queueFluid(FluidStack fluid, ForgeDirection from, int destination, IAdditionalTargetInformation info)` — line 233
- `private ForgeDirection safeDirection(ForgeDirection direction)` — line 244
- `private void extractionSucceeded()` — line 248

## logisticspipes.crafting.PatternSatelliteDispatchHandler

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:31`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, AdjacentInventoryHandler adjacentInventory)` — line 37
- `DispatchPlan findInsertableBufferedPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern, int maxSets)` — line 47
- `int insertedSetsFromPlan(ItemStack pattern, List<PatternIngredientAssignment> plan)` — line 67
- `int maxDispatchableSets(PatternCraftingReference ownerReference, ItemStack pattern, int maxSets)` — line 89
- `private List<PatternIngredientAssignment> buildPatternAssignments(ItemStack pattern, int sets)` — line 109
- `private DispatchPlan buildDispatchPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern, List<PatternIngredientAssignment> assignments)` — line 133
- `DispatchPlan readFromNBT(NBTTagCompound tag)` — line 184
- `private static NBTTagList writeAssignments(List<PatternIngredientAssignment> assignments)` — line 234
- `private static List<PatternIngredientAssignment> readAssignments(NBTTagList list)` — line 245
- `private List<PipeItemsPatternSatelliteLogistics> uniqueItemSatellites(List<ItemSatelliteAssignment> assignments)` — line 257
- `private List<PipeFluidPatternSatelliteLogistics> uniqueFluidSatellites(List<FluidSatelliteAssignment> assignments)` — line 267

## logisticspipes.crafting.PatternSatelliteDispatchHandler.ItemSatelliteAssignment

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:277`

- `private <init>(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack, int inputSlot)` — line 288
- `private <init>(String satelliteUuid, ItemIdentifierStack stack, int inputSlot)` — line 294

## logisticspipes.crafting.PatternSatelliteDispatchHandler.FluidSatelliteAssignment

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:302`

- `private <init>(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid, int amount)` — line 311
- `private <init>(String satelliteUuid, FluidIdentifier fluid, int amount)` — line 317

## logisticspipes.crafting.PatternSatelliteDispatchHandler.DispatchResult

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:325`

No explicit method declarations.

## logisticspipes.crafting.PatternSatelliteDispatchHandler.DispatchPlan

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:341`

- `private <init>(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern, List<PatternIngredientAssignment> assignments)` — line 356
- `private <init>(PatternCraftingReference ownerReference, PatternCraftingReference batchReference, int patternSlot, ItemStack pattern, List<PatternIngredientAssignment> assignments, ForgeDirection localDirection)` — line 368
- `List<PatternIngredientAssignment> assignments()` — line 379
- `int patternSlot()` — line 383
- `PatternCraftingReference ownerReference()` — line 387
- `PatternCraftingReference batchReference()` — line 391
- `ItemStack pattern()` — line 395
- `logisticspipes.utils.AdjacentTile localTarget()` — line 399
- `private List<Object> targetInventories()` — line 406
- `boolean sameRecipe(DispatchPlan other)` — line 442
- `boolean sharesTargets(DispatchPlan other)` — line 468
- `private void addLocal(PatternIngredientAssignment assignment)` — line 474
- `boolean usesLocalInventory()` — line 479
- `int sets()` — line 483
- `boolean resolveTargets()` — line 488
- `NBTTagCompound writeToNBT()` — line 504
- `private void addItemSatellite(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack, int inputSlot)` — line 542
- `private boolean addFluidSatellite(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid, int amount)` — line 550
- `boolean canDispatch()` — line 566
- `DispatchResult dispatch(PatternStackBufferHandler buffer)` — line 628
- `private void insertLocal(PatternStackBufferHandler buffer)` — line 710
- `private boolean isComplete()` — line 733
- `boolean abandon()` — line 755
- `boolean release()` — line 779
- `PatternCraftingBlockingHandler.SatelliteBatch satelliteBatch()` — line 787
- `private boolean hasSatellites()` — line 794
- `private boolean usesSatelliteReservations()` — line 798
- `private boolean canRouteToItemSatellite(ItemSatelliteAssignment assignment)` — line 803
- `private void routeItemSatelliteAssignment(ItemSatelliteAssignment assignment, boolean reserveSatellites)` — line 810
- `private boolean reserveSatellites(List<PipeItemsPatternSatelliteLogistics> itemSatellites, List<PipeFluidPatternSatelliteLogistics> fluidSatellites)` — line 829
- `private void releaseSatellites(List<PipeItemsPatternSatelliteLogistics> itemSatellites, List<PipeFluidPatternSatelliteLogistics> fluidSatellites)` — line 846

## logisticspipes.crafting.PatternSatelliteDispatchHandler.SatelliteDispatchBatch

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteDispatchHandler.java:857`

- `private <init>(DispatchPlan plan)` — line 861
- `@Override public PatternCraftingReference ownerReference()` — line 865
- `@Override public int patternSlot()` — line 870
- `@Override public boolean isConsumed()` — line 875
- `@Override public int size()` — line 891
- `@Override public void retrieveAndRelease()` — line 901
- `@Override public void release()` — line 906
- `@Override public NBTTagCompound writeToNBT()` — line 911

## logisticspipes.crafting.PatternSatelliteInfo

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteInfo.java:11`

- `public <init>(int id, int x, int y, int z, int dimension, int distance, boolean favorite, String uuid, String displayName)` — line 26
- `public <init>(int id, int x, int y, int z, int dimension, int distance, boolean favorite, String uuid, String displayName, SatelliteType type)` — line 31
- `public static PatternSatelliteInfo readData(LPDataInputStream data)` — line 47
- `public String getSearchText()` — line 61
- `public void writeData(LPDataOutputStream data)` — line 91
- `public SatelliteType type()` — line 104
- `public int id()` — line 108
- `public int x()` — line 112
- `public int y()` — line 116
- `public int z()` — line 120
- `public int dimension()` — line 124
- `public int distance()` — line 128
- `public boolean favorite()` — line 132
- `public String uuid()` — line 136
- `public String displayName()` — line 140
- `@Override public boolean equals(Object obj)` — line 144
- `@Override public int hashCode()` — line 160
- `@Override public String toString()` — line 165

## logisticspipes.crafting.PatternSatelliteInfo.SatelliteType

Source: `src/main/java/logisticspipes/crafting/PatternSatelliteInfo.java:199`

No explicit method declarations.

## logisticspipes.crafting.PatternStackBufferHandler

Source: `src/main/java/logisticspipes/crafting/PatternStackBufferHandler.java:26`

- `<init>(Runnable changeListener)` — line 38
- `int amount(int patternSlot, IPatternStack stack)` — line 42
- `private static int removeMatching(List<IPatternStack> stacks, int amount, Predicate<IPatternStack> matcher)` — line 55
- `private static List<IPatternStack> copyStacks(List<IPatternStack> stacks)` — line 73
- `int amount(int patternSlot, ItemIdentifier item)` — line 83
- `int amount(int patternSlot, FluidIdentifier fluid)` — line 87
- `private int amountMatching(int patternSlot, Predicate<IPatternStack> matcher)` — line 91
- `void add(PatternCraftingReference owner, int patternSlot, IPatternStack stack)` — line 101
- `List<PatternCraftingReference> owners(int patternSlot)` — line 114
- `List<IPatternStack> copyOwnedStacks(PatternCraftingReference owner)` — line 124
- `void remove(PatternCraftingReference owner, IPatternStack stack, int amount)` — line 129
- `List<IPatternStack> removeAll(PatternCraftingReference owner)` — line 149
- `List<OwnedEntry> entries(UUID instanceId)` — line 163
- `private List<IPatternStack> getExistingBuffer(int patternSlot)` — line 173
- `public List<IPatternStack> removeAll(int patternSlot)` — line 178
- `private List<IPatternStack> getOrCreateBuffer(int patternSlot)` — line 188
- `public void dropContents(World world, int x, int y, int z)` — line 192
- `private void cleanupAggregate(int patternSlot)` — line 205
- `public int size()` — line 216
- `public void clear()` — line 220
- `long changeVersion()` — line 229
- `@Override public void readFromNBT(NBTTagCompound tag)` — line 233
- `public Map<Integer, List<IPatternStack>> asMap()` — line 249
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 253
- `public List<Integer> keySet()` — line 268
- `private void markChanged()` — line 272
- `long unreservedAmount(java.util.Set<UUID> admitted, boolean fluid)` — line 300
- `static List<ItemStack> makeItemStacks(IPatternStack patternStack)` — line 310

## logisticspipes.crafting.PatternStackBufferHandler.OwnedStacks

Source: `src/main/java/logisticspipes/crafting/PatternStackBufferHandler.java:279`

- `private <init>(int patternSlot)` — line 284

## logisticspipes.crafting.PatternStackBufferHandler.OwnedEntry

Source: `src/main/java/logisticspipes/crafting/PatternStackBufferHandler.java:289`

- `private <init>(PatternCraftingReference owner, int patternSlot)` — line 294

## logisticspipes.crafting.PatternStackRequestHandler

Source: `src/main/java/logisticspipes/crafting/PatternStackRequestHandler.java:28`

- `boolean hasInstance(UUID instance)` — line 39
- `long unreservedAmount(java.util.Set<UUID> admitted, boolean fluid)` — line 45
- `<init>(Map<Integer, List<IPatternStack>> requestedIngredients, Runnable changeListener)` — line 55
- `private static int removeMatching(List<IPatternStack> stacks, int amount, Predicate<IPatternStack> matcher)` — line 60
- `int amount(int patternSlot, IPatternStack stack)` — line 78
- `int amount(PatternCraftingReference owner, IPatternStack stack)` — line 82
- `int amountMatching(PatternCraftingReference owner, Predicate<IPatternStack> matcher)` — line 99
- `int amount(int patternSlot, ItemIdentifier item)` — line 113
- `int amountMatching(int patternSlot, Predicate<IPatternStack> matcher)` — line 117
- `int amount(int patternSlot, FluidIdentifier fluid)` — line 127
- `void add(PatternCraftingReference owner, int patternSlot, IPatternStack stack)` — line 131
- `void remove(PatternCraftingReference owner, int patternSlot, IPatternStack stack)` — line 144
- `int removeMatching(PatternCraftingReference owner, int patternSlot, int amount, Predicate<IPatternStack> matcher)` — line 162
- `boolean removeAll(PatternCraftingReference owner)` — line 198
- `boolean removeInstance(UUID instanceId)` — line 211
- `boolean removeAll(int patternSlot)` — line 221
- `List<OwnedEntry> entries()` — line 230
- `private void cleanup(PatternCraftingReference owner, OwnedStacks owned)` — line 240
- `private void cleanupAggregate(int patternSlot)` — line 247
- `private List<IPatternStack> getExistingRequested(int patternSlot)` — line 258
- `private List<IPatternStack> getOrCreateRequested(int patternSlot)` — line 263
- `@Override public void readFromNBT(NBTTagCompound tag)` — line 267
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 283
- `private void markChanged()` — line 301

## logisticspipes.crafting.PatternStackRequestHandler.OwnedEntry

Source: `src/main/java/logisticspipes/crafting/PatternStackRequestHandler.java:307`

- `private <init>(PatternCraftingReference owner, int patternSlot, IPatternStack stack)` — line 313

## logisticspipes.crafting.PatternStackRequestHandler.OwnedStacks

Source: `src/main/java/logisticspipes/crafting/PatternStackRequestHandler.java:320`

- `private <init>(int patternSlot)` — line 325

## logisticspipes.crafting.PatternStagedCraftingCoordinator

Source: `src/main/java/logisticspipes/crafting/PatternStagedCraftingCoordinator.java:41`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, PatternHandler patternHandler, PatternStackRequestHandler requestedIngredient)` — line 63
- `static List<PatternCraftingMonitorEntry> pendingMonitorEntries(NBTTagCompound tag, int restoreAttempts, int maxRestoreAttempts)` — line 72
- `static int assignMissingStandaloneReferences(NBTTagCompound tag)` — line 91
- `static Set<UUID> pendingInstanceIds(NBTTagCompound tag)` — line 100
- `boolean hasPattern(int patternSlot)` — line 108
- `boolean queuedForWorkspace(int slot)` — line 117
- `void writeToNBT(NBTTagCompound tag)` — line 123
- `static boolean removePendingInstance(NBTTagCompound tag, UUID instanceId)` — line 144
- `static boolean hasPendingOrders(NBTTagCompound tag)` — line 160
- `static int incomingAmount(NBTTagCompound tag, PatternCraftingReference owner, IPatternStack ingredient)` — line 166
- `private static int incomingOrderAmount(NBTTagCompound tag, PatternCraftingReference owner, IPatternStack ingredient)` — line 183
- `private static void appendPendingStagedOrders(NBTTagList list, Map<UUID, List<PatternCraftingMonitorNode>> rootsByInstance)` — line 195
- `private static int assignMissingStandaloneReferences(NBTTagList list)` — line 202
- `private static void appendPendingStagedInstanceIds(NBTTagList list, Set<UUID> result)` — line 215
- `private static void appendPendingInstanceIds(NBTTagList list, Set<UUID> result)` — line 221
- `private static void appendPendingInstanceId(NBTTagCompound orderTag, Set<UUID> result)` — line 227
- `private static void appendPendingOrders(NBTTagList list, Map<UUID, List<PatternCraftingMonitorNode>> rootsByInstance)` — line 234
- `private static void appendPendingOrder(NBTTagCompound orderTag, Map<UUID, List<PatternCraftingMonitorNode>> rootsByInstance)` — line 241
- `void appendDebugState(StringBuilder out, String prefix)` — line 252
- `private static boolean removePendingOrders(NBTTagList list, UUID instanceId, boolean staged)` — line 278
- `IOrderInfoProvider fulfill(IPromise promise, IResource requestType, IAdditionalTargetInformation info, PatternCraftingBranch branch)` — line 292
- `void requestIngredients()` — line 338
- `void requestIngredientsAfterCapacityChange()` — line 343
- `private void removeOutputOrder(IOrderInfoProvider order)` — line 348
- `private boolean hasRequestTarget(IPromise promise, IResource requestType)` — line 356
- `private IRequestItems getRequestTarget(IResource requestType)` — line 363
- `private int resolvePatternSlot(IPromise promise)` — line 373
- `private int resolveResultAmountPerSet(IPromise promise, int patternSlot)` — line 383
- `private NBTTagList writeStagedOrders(Set<IOrderInfoProvider> savedOutputOrders, Map<PatternCraftingReference, NBTTagCompound> records)` — line 393
- `boolean restoreFromNBT(NBTTagCompound tag)` — line 429
- `int remainingSets(int patternSlot)` — line 501
- `int remainingOutputAmount(int patternSlot, IPatternStack output)` — line 511
- `private List<RestoredStagedOrder> readStagedOrders(NBTTagList list, Map<PatternCraftingReference, PatternCraftingBranch> records)` — line 530
- `private static PatternCraftingBranch resolveBranch(Map<PatternCraftingReference, PatternCraftingBranch> records, PatternCraftingReference reference)` — line 557
- `private List<PatternCraftingPersistence.RestoredOrder> readOrders(NBTTagList list)` — line 564
- `private void cleanupCompletedOutputOrders()` — line 579
- `void releaseAll()` — line 594
- `Set<UUID> instancesForPattern(int patternSlot)` — line 609
- `boolean cancelTrackedOrder(PatternCraftingOrder order)` — line 619
- `private void registerOrder(PatternCraftingReference reference, int patternSlot, int resultAmountPerSet, PatternCraftingBranch branch, IOrderInfoProvider order)` — line 638
- `private NBTTagList writeStandaloneItemOrders(Set<IOrderInfoProvider> savedOutputOrders)` — line 665
- `private NBTTagList writeStandaloneFluidOrders(Set<IOrderInfoProvider> savedOutputOrders)` — line 680
- `private void ensureStandaloneReference(LogisticsOrder order)` — line 695

## logisticspipes.crafting.PatternStagedCraftingCoordinator.RestoredStagedOrder

Source: `src/main/java/logisticspipes/crafting/PatternStagedCraftingCoordinator.java:707`

No explicit method declarations.

## logisticspipes.crafting.PatternStagedCraftingScheduler

Source: `src/main/java/logisticspipes/crafting/PatternStagedCraftingScheduler.java:20`

- `<init>(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe, List<PatternCraftingOrder> stagedCrafts)` — line 28
- `void requestIngredients(boolean capacityChanged)` — line 38
- `void requestIngredients(int patternSlot)` — line 59
- `private void requestIngredientsGuarded(int patternSlot)` — line 71
- `private boolean removeFinishedOrder(PatternCraftingOrder order)` — line 88
- `private boolean removeOrderWithoutPattern(PatternCraftingOrder order, ItemStack pattern)` — line 112
- `private boolean removeFullyRequestedOrder(PatternCraftingOrder order)` — line 126
- `private void requestOrderIngredients(PatternCraftingOrder order, ItemStack pattern)` — line 141
- `private int orderableSetsForPattern(PatternCraftingOrder order, ItemStack pattern, int branchSets)` — line 201

## logisticspipes.crafting.PatternTargetInformation

Source: `src/main/java/logisticspipes/crafting/PatternTargetInformation.java:7`

- `public <init>(int patternSlot)` — line 14
- `public <init>(int patternSlot, int inputSlot)` — line 18
- `public static PatternTargetInformation delivery(int patternSlot, int inputSlot, PatternCraftingReference orderReference)` — line 22
- `public boolean isTracked()` — line 30
- `public static PatternTargetInformation batchOutput(int patternSlot, int outputSlot, PatternCraftingReference batch)` — line 34
- `public boolean isBatchOutput()` — line 39
- `public int outputSlot()` — line 43

## logisticspipes.crafting.PipeFluidPatternSatelliteLogistics

Source: `src/main/java/logisticspipes/crafting/PipeFluidPatternSatelliteLogistics.java:35`

- `public <init>(Item item)` — line 50
- `public static void cleanup()` — line 54
- `public static PipeFluidPatternSatelliteLogistics findById(int satelliteId)` — line 58
- `public static PipeFluidPatternSatelliteLogistics findById(int satelliteId, IRouter requester)` — line 62
- `public static PipeFluidPatternSatelliteLogistics findByUuid(String satelliteUuid)` — line 75
- `static List<PatternByproductExtractionTarget> getRegisteredByproductExtractionTargets()` — line 87
- `public static List<PatternSatelliteInfo> getKnownSatellitesFor(EntityPlayer player)` — line 91
- `private static boolean isSelectableSatellite(PipeFluidPatternSatelliteLogistics satellite)` — line 128
- `private static int getDistance(EntityPlayer player, int playerDimension, PipeFluidPatternSatelliteLogistics satellite, int satelliteDimension)` — line 135
- `public String getSatelliteUuid()` — line 146
- `public String getDisplayName()` — line 150
- `@Override public String getSatelliteName()` — line 155
- `@Override public boolean canExtractByproductsFor(IRouter requester)` — line 160
- `@Override public PatternByproductExtractionResult extractItemByproduct(ItemIdentifier item, int amount, int destination, IAdditionalTargetInformation info)` — line 165
- `@Override public PatternByproductExtractionResult extractFluidByproduct(FluidIdentifier fluid, int amount, int destination, IAdditionalTargetInformation info)` — line 171
- `public void setSatelliteName(String satelliteName)` — line 177
- `public boolean canReserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 189
- `public boolean reserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 200
- `public void releaseReservation(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 213
- `public boolean isReservationConsumed(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 228
- `public boolean canAcceptPatternInput(FluidIdentifier fluid, int amount)` — line 245
- `public boolean canAcceptPatternInputs(List<PatternFluidStack> fluids)` — line 250
- `List<TileEntity> patternTargetTanks()` — line 260
- `public boolean isPatternTargetEmpty()` — line 269
- `public int insertPatternInput(FluidIdentifier fluid, int amount)` — line 292
- `public int insertPatternInput(FluidIdentifier fluid, int amount, boolean trackReservation)` — line 299
- `public int retrieveFluidToStorage(FluidIdentifier fluid, int amount)` — line 317
- `private void queueFluidToStorage(FluidStack fluid, ForgeDirection from)` — line 359
- `private int fillPatternInput(FluidIdentifier fluid, int amount, boolean doFill)` — line 367
- `private int countAdjacentFluid(FluidIdentifier fluid)` — line 386
- `private UUID ownerRouterId(PipeItemsPatternCraftingLogistics owner)` — line 408
- `private void markReservationDirty()` — line 412
- `@Override public void enabledUpdateEntity()` — line 416
- `@Override protected void ensureAllSatelliteStatus()` — line 424
- `@Override public void setSatelliteId(int satelliteId)` — line 441
- `@Override public void onWrenchClicked(EntityPlayer entityplayer)` — line 447
- `@Override public void readFromNBT(NBTTagCompound nbttagcompound)` — line 453
- `@Override public void writeToNBT(NBTTagCompound nbttagcompound)` — line 474
- `@Override public void onAllowedRemoval()` — line 494
- `private void ensureUniqueDisplayNameInNetwork()` — line 502
- `private boolean hasDisplayNameConflict(String displayName)` — line 517
- `private boolean isInSameNetwork(PipeFluidPatternSatelliteLogistics other)` — line 529

## logisticspipes.crafting.PipeItemsPatternSatelliteLogistics

Source: `src/main/java/logisticspipes/crafting/PipeItemsPatternSatelliteLogistics.java:49`

- `public <init>(Item item)` — line 71
- `public static void cleanup()` — line 75
- `public static PipeItemsPatternSatelliteLogistics findById(int satelliteId)` — line 79
- `public static PipeItemsPatternSatelliteLogistics findById(int satelliteId, IRouter requester)` — line 83
- `public static PipeItemsPatternSatelliteLogistics findByUuid(String satelliteUuid)` — line 96
- `static List<PatternByproductExtractionTarget> getRegisteredByproductExtractionTargets()` — line 108
- `public static List<Integer> getKnownSatelliteIds()` — line 112
- `public static List<PatternSatelliteInfo> getKnownSatellitesFor(EntityPlayer player)` — line 122
- `private static boolean isSelectableSatellite(PipeItemsPatternSatelliteLogistics satellite)` — line 164
- `private static Set<Integer> getFavoriteSatelliteIds(EntityPlayer player)` — line 171
- `private static Set<String> getFavoriteSatelliteUuids(EntityPlayer player)` — line 186
- `private static int getDistance(EntityPlayer player, int playerDimension, PipeItemsPatternSatelliteLogistics satellite, int satelliteDimension)` — line 201
- `public String getDisplayName()` — line 212
- `@Override public String getSatelliteName()` — line 217
- `@Override public boolean canExtractByproductsFor(IRouter requester)` — line 222
- `@Override public PatternByproductExtractionResult extractItemByproduct(ItemIdentifier item, int amount, int destination, IAdditionalTargetInformation info)` — line 227
- `@Override public PatternByproductExtractionResult extractFluidByproduct(FluidIdentifier fluid, int amount, int destination, IAdditionalTargetInformation info)` — line 233
- `@Override public boolean isLockedExit(ForgeDirection orientation)` — line 242
- `@Override public void enabledUpdateEntity()` — line 251
- `@Override public void setSatelliteName(String satelliteName)` — line 259
- `public boolean canReserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 272
- `public boolean reserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 283
- `public void releaseReservation(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 296
- `public boolean isReservationConsumed(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference)` — line 312
- `private boolean hasActivePatternInputReservation()` — line 331
- `public boolean canAcceptPatternInput(ItemIdentifierStack stack)` — line 346
- `public boolean canAcceptPatternInputs(List<ItemIdentifierStack> stacks)` — line 351
- `public boolean isPatternTargetEmpty()` — line 368
- `void expectStagedPatternInput(ItemIdentifierStack stack, PatternCraftingReference delivery, boolean tracked)` — line 395
- `int stagedPatternInputAmount(PatternCraftingReference delivery)` — line 400
- `int insertStagedPatternInput(PatternCraftingReference delivery, int amount)` — line 405
- `public void expectPatternInput(ItemIdentifierStack stack, PatternCraftingReference delivery, boolean trackReservation)` — line 420
- `@Override public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 434
- `@Override public void throttledUpdateEntity()` — line 452
- `private void trimCompletedDeliveries()` — line 478
- `public int insertPatternInput(ItemIdentifierStack stack)` — line 490
- `public int insertPatternInput(ItemIdentifierStack stack, boolean trackReservation)` — line 497
- `public int retrieveOrCancelToStorage(ItemIdentifierStack stack, boolean interceptMissing, PatternCraftingReference deliveryReference)` — line 523
- `@Override public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 548
- `private void markExpectedInputArrived(ItemIdentifierStack item)` — line 595
- `private int retrieveLandedItemsToStorage(ItemIdentifierStack stack)` — line 612
- `private int roomForPatternInput(ItemIdentifierStack stack)` — line 640
- `private int countAdjacentItem(ItemIdentifier item)` — line 654
- `AdjacentTile getPatternTargetInventory()` — line 671
- `private IInventory getInsertableInventory(AdjacentTile target)` — line 690
- `private UUID ownerRouterId(PipeItemsPatternCraftingLogistics owner)` — line 703
- `private void markReservationDirty()` — line 707
- `private void queueToStorage(ItemStack stack, ForgeDirection from)` — line 711
- `private void addPendingCancelledArrival(ItemIdentifier item, int amount, PatternCraftingReference deliveryReference)` — line 722
- `private int removePendingCancelledArrival(ItemIdentifierStack arriving, IAdditionalTargetInformation info)` — line 740
- `private void purgeExpiredCancelledArrivals()` — line 760
- `@Override protected int findId(int increment)` — line 774
- `@Override protected void ensureAllSatelliteStatus()` — line 797
- `private void ensureUniqueDisplayNameInNetwork()` — line 813
- `private boolean hasDisplayNameConflict(String displayName)` — line 828
- `private boolean isInSameNetwork(PipeItemsPatternSatelliteLogistics other)` — line 840
- `@Override public boolean handleClick(EntityPlayer player, SecuritySettings settings)` — line 851
- `@Override public void setSatelliteId(int satelliteId)` — line 877
- `@Override public void onWrenchClicked(EntityPlayer entityplayer)` — line 883
- `@Override public void readFromNBT(NBTTagCompound nbttagcompound)` — line 889
- `@Override public void writeToNBT(NBTTagCompound nbttagcompound)` — line 945
- `@Override public void onAllowedRemoval()` — line 988

## logisticspipes.crafting.PipeItemsPatternSatelliteLogistics.PendingCancelledArrival

Source: `src/main/java/logisticspipes/crafting/PipeItemsPatternSatelliteLogistics.java:1002`

- `private <init>(ItemIdentifier item, int amount, long expires, PatternCraftingReference deliveryReference)` — line 1009
- `private boolean matches(ItemIdentifier item, PatternCraftingReference deliveryReference)` — line 1017
- `private boolean matches(ItemIdentifierStack arriving, IAdditionalTargetInformation info)` — line 1021

## logisticspipes.crafting.SatelliteInventoryClearer

Source: `src/main/java/logisticspipes/crafting/SatelliteInventoryClearer.java:24`

- `private <init>()` — line 26
- `public static void clearInventory(CoreRoutedPipe pipe)` — line 28
- `private static boolean matches(FluidStack stack, FluidIdentifier fluid)` — line 104
- `private static int fluidBatch(long remaining)` — line 108
- `private static void queueFluid(CoreRoutedPipe pipe, FluidStack fluid, ForgeDirection from)` — line 112
- `private static void queue(CoreRoutedPipe pipe, IRoutedItem item, ForgeDirection from)` — line 120

## logisticspipes.crafting.pattern.AbstractPattern

Source: `src/main/java/logisticspipes/crafting/pattern/AbstractPattern.java:20`

- `protected <init>(ItemStack patternStack)` — line 38
- `public abstract int getIngredientSlotCount()` — line 42
- `public abstract int getResultSlotCount()` — line 44
- `public int getResultSlotStart()` — line 46
- `public int getItemSlotCount()` — line 50
- `public List<IPatternStack> getAggregatedInputs()` — line 54
- `public List<IPatternStack> getAggregatedOutputs()` — line 58
- `public int getMainOutputSlot()` — line 63
- `public void setMainOutputSlot(int slot)` — line 80
- `public List<IPatternStack> getCraftableOutputs()` — line 88
- `public List<ItemIdentifierStack> getAggregatedIngredients()` — line 97
- `public List<PatternFluidStack> getAggregatedFluidIngredients()` — line 101
- `public void clear()` — line 108
- `public void multiply(int factor)` — line 128
- `public ItemStack getStackInSlot(int slot)` — line 149
- `public IPatternStack getPatternStackInSlot(int slot)` — line 154
- `public void setStackInSlot(int slot, ItemStack stack)` — line 168
- `public void setPatternStackInSlot(int slot, IPatternStack stack)` — line 172
- `public boolean isOreDictSubstitutionEnabled()` — line 201
- `public void setOreDictSubstitutionEnabled(boolean enabled)` — line 208
- `public void toggleOreDictSubstitution()` — line 215
- `public boolean isIgnoreNbtEnabled()` — line 222
- `public void setIgnoreNbtEnabled(boolean enabled)` — line 229
- `public void toggleIgnoreNbt()` — line 236
- `public int getSatelliteIdForInputSlot(int slot)` — line 240
- `public String getSatelliteUuidForInputSlot(int slot)` — line 244
- `public void setSatelliteIdForInputSlot(int slot, int satelliteId)` — line 248
- `public void setSatelliteTargetForInputSlot(int slot, int satelliteId, String satelliteUuid)` — line 252
- `public int getFluidSatelliteIdForInputSlot(int slot)` — line 262
- `public String getFluidSatelliteUuidForInputSlot(int slot)` — line 266
- `public void setFluidSatelliteIdForInputSlot(int slot, int satelliteId)` — line 270
- `public void setFluidSatelliteTargetForInputSlot(int slot, int satelliteId, String satelliteUuid)` — line 274
- `public int getByproductSatelliteIdForOutputSlot(int slot)` — line 284
- `public String getByproductSatelliteUuidForOutputSlot(int slot)` — line 288
- `public void setByproductSatelliteIdForOutputSlot(int slot, int satelliteId)` — line 292
- `public void setByproductSatelliteTargetForOutputSlot(int slot, int satelliteId, String satelliteUuid)` — line 296
- `public int getFluidByproductSatelliteIdForOutputSlot(int slot)` — line 306
- `public String getFluidByproductSatelliteUuidForOutputSlot(int slot)` — line 310
- `public void setFluidByproductSatelliteIdForOutputSlot(int slot, int satelliteId)` — line 314
- `public void setFluidByproductSatelliteTargetForOutputSlot(int slot, int satelliteId, String satelliteUuid)` — line 318
- `private int getSatelliteId(int slot, String targetTag, int slotCount)` — line 328
- `private String getSatelliteUuid(int slot, String uuidTag, int slotCount)` — line 336
- `private void setSatelliteTarget(int slot, int satelliteId, String satelliteUuid, String targetTag, String uuidTag, int slotCount)` — line 344
- `public List<IPatternStack> getInputs()` — line 364
- `public List<IPatternStack> getOutputs()` — line 368
- `public List<ItemIdentifierStack> getIngredients()` — line 372
- `public List<ItemIdentifierStack> getResults()` — line 376
- `public List<PatternFluidStack> getFluidIngredients()` — line 380
- `public List<PatternFluidStack> getFluidResults()` — line 384
- `public void setFluidIngredients(List<PatternFluidStack> fluids)` — line 388
- `public void setFluidResults(List<PatternFluidStack> fluids)` — line 392
- `public ItemStack getPrimaryResultStack()` — line 396
- `public boolean isConfigured()` — line 404
- `public void addTooltipInformation(List<String> tooltip)` — line 410
- `private void addPatternStacksToTooltip(List<String> tooltip, List<IPatternStack> stacks, ChatColor color)` — line 442
- `private List<ItemIdentifierStack> toItemIdentifierStacks(List<PatternItemStack> stacks)` — line 459
- `private List<PatternItemStack> getSolidPatternStacks(List<IPatternStack> stacks)` — line 467
- `private List<PatternFluidStack> getFluidPatternStacks(List<IPatternStack> stacks)` — line 477
- `private List<PatternFluidStack> readFluidRange(int start, int end)` — line 487
- `private List<PatternItemStack> readSolidRange(int start, int end)` — line 498
- `private List<IPatternStack> readPatternRange(int start, int end)` — line 509
- `private void setFluidStacksInRange(int start, int end, List<PatternFluidStack> fluids)` — line 520
- `private boolean getBooleanTag(String tagName)` — line 532
- `private void setBooleanTag(String tagName, boolean enabled)` — line 537
- `private NBTTagCompound getOrCreateTag(ItemStack stack)` — line 549
- `public boolean canSetInputsAndOutputs(List<IPatternStack> inputs, List<Integer> indices, List<IPatternStack> outputs)` — line 557
- `public void setInputsAndOutputs(@NonNull List<IPatternStack> inputs, @NonNull List<Integer> indices, @NonNull List<IPatternStack> outputs)` — line 580

## logisticspipes.crafting.pattern.DefaultPattern

Source: `src/main/java/logisticspipes/crafting/pattern/DefaultPattern.java:5`

- `public <init>(ItemStack patternStack)` — line 11
- `@Override public int getIngredientSlotCount()` — line 15
- `@Override public int getResultSlotCount()` — line 20

## logisticspipes.crafting.pattern.EditedPatternInventory

Source: `src/main/java/logisticspipes/crafting/pattern/EditedPatternInventory.java:12`

- `public <init>(PatternSource source, int patternSlot)` — line 17
- `public void setPatternSlot(int patternSlot)` — line 22
- `public ItemStack getPatternStack()` — line 26
- `@Override public int getSizeInventory()` — line 30
- `@Override public ItemStack getStackInSlot(int slot)` — line 35
- `@Override public ItemStack decrStackSize(int slot, int count)` — line 40
- `@Override public ItemStack getStackInSlotOnClosing(int slot)` — line 51
- `@Override public void setInventorySlotContents(int slot, ItemStack stack)` — line 56
- `@Override public String getInventoryName()` — line 62
- `@Override public boolean hasCustomInventoryName()` — line 67
- `@Override public int getInventoryStackLimit()` — line 72
- `@Override public void markDirty()` — line 77
- `@Override public boolean isUseableByPlayer(EntityPlayer player)` — line 82
- `@Override public boolean isItemValidForSlot(int slot, ItemStack stack)` — line 87
- `@Override public void openInventory()` — line 93
- `@Override public void closeInventory()` — line 96

## logisticspipes.crafting.pattern.ItemPattern

Source: `src/main/java/logisticspipes/crafting/pattern/ItemPattern.java:25`

- `public <init>()` — line 36
- `public static AbstractPattern fromStack(ItemStack pattern)` — line 40
- `public static boolean isProcessingPattern(ItemStack pattern)` — line 47
- `public static void setProcessingPattern(ItemStack pattern, boolean processing)` — line 58
- `public static void toggleProcessingPattern(ItemStack pattern)` — line 78
- `public AbstractPattern createPattern(ItemStack pattern)` — line 82
- `@Override public void registerIcons(IIconRegister register)` — line 89
- `@Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)` — line 94
- `@Override @SideOnly(Side.CLIENT) public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel)` — line 102
- `@Override public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings)` — line 108
- `@Override public boolean addShiftInfo()` — line 113
- `@Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)` — line 118

## logisticspipes.crafting.pattern.PatternHandler

Source: `src/main/java/logisticspipes/crafting/pattern/PatternHandler.java:21`

- `public <init>(SimpleStackInventory patternInventory)` — line 37
- `public int size()` — line 41
- `public void invalidate()` — line 46
- `public long getChangeVersion()` — line 51
- `public ItemStack getConfiguredPatternStack(int slot)` — line 55
- `public List<ItemStack> getConfiguredPatterns()` — line 63
- `public PatternRecipeSnapshot getRecipe(int slot)` — line 68
- `public PatternRecipeSnapshot getRecipe(ItemStack pattern)` — line 76
- `public Set<ItemIdentifier> getIngredientItems()` — line 89
- `public Set<ItemIdentifier> getOutputItems(boolean fluidCraftingSupported)` — line 95
- `public Set<ItemIdentifier> getCraftedItems(boolean fluidCraftingSupported)` — line 100
- `public List<ItemIdentifierStack> getCraftResults(boolean fluidCraftingSupported)` — line 105
- `public boolean isIngredient(ItemIdentifier item)` — line 110
- `boolean isFluidIngredient(FluidIdentifier fluid)` — line 118
- `public int findPatternSlotForResult(ItemIdentifier item)` — line 127
- `public int resultAmount(int patternSlot, ItemIdentifier item)` — line 140
- `public int fluidIngredientAmount(ItemStack pattern, FluidIdentifier fluid)` — line 154
- `public List<IPatternStack> getAggregatedInputs(ItemStack pattern)` — line 159
- `private void ensureCache()` — line 164

## logisticspipes.crafting.pattern.PatternRecipeImport

Source: `src/main/java/logisticspipes/crafting/pattern/PatternRecipeImport.java:20`

- `public <init>(boolean processing, List<IPatternStack> inputs, List<Integer> inputSlots, List<IPatternStack> outputs)` — line 37
- `public boolean isProcessing()` — line 48
- `public List<IPatternStack> getInputs()` — line 52
- `public List<Integer> getInputSlots()` — line 56
- `public List<IPatternStack> getOutputs()` — line 60
- `public boolean isEmpty()` — line 64
- `public void applyTo(ItemStack patternStack)` — line 73
- `private static IPatternStack clampAmount(IPatternStack stack)` — line 96
- `public NBTTagCompound writeToNBT()` — line 108
- `public static PatternRecipeImport readFromNBT(NBTTagCompound tag)` — line 129

## logisticspipes.crafting.pattern.PatternRecipeSnapshot

Source: `src/main/java/logisticspipes/crafting/pattern/PatternRecipeSnapshot.java:22`

- `<init>(ItemStack patternStack)` — line 43
- `public ItemStack getPatternStack()` — line 97
- `public AbstractPattern getPattern()` — line 101
- `public boolean isConfigured()` — line 105
- `public List<IPatternStack> getInputs()` — line 109
- `public List<IPatternStack> getOutputs()` — line 113
- `public List<IPatternStack> getAggregatedInputs()` — line 117
- `public IPatternStack getInput(int slot)` — line 121
- `public int getIngredientSlotCount()` — line 125
- `public IPatternStack getOutput(int slot)` — line 129
- `public int getMainOutputSlot()` — line 133
- `public List<IPatternStack> getCraftableOutputs()` — line 137
- `public int getResultSlotCount()` — line 142
- `public int getItemSatelliteId(int slot)` — line 146
- `public String getItemSatelliteUuid(int slot)` — line 150
- `public int getFluidSatelliteId(int slot)` — line 154
- `public String getFluidSatelliteUuid(int slot)` — line 158
- `public int getByproductSatelliteId(int slot)` — line 162
- `public String getByproductSatelliteUuid(int slot)` — line 166
- `public int getFluidByproductSatelliteId(int slot)` — line 170
- `public String getFluidByproductSatelliteUuid(int slot)` — line 174
- `public boolean containsFluid()` — line 178
- `public int getFluidIngredientAmount(FluidIdentifier fluid)` — line 182
- `public boolean isOreDictSubstitutionEnabled()` — line 186
- `public boolean isIgnoreNbtEnabled()` — line 190

## logisticspipes.crafting.pattern.PatternSource

Source: `src/main/java/logisticspipes/crafting/pattern/PatternSource.java:15`

- `int getPatternCount()` — line 20
- `ItemStack getPatternStack(int slot)` — line 25
- `void markPatternChanged(int slot)` — line 30
- `default void onSatelliteAssigned(int satelliteId, String satelliteUuid, boolean fluid)` — line 35
- `static boolean isPattern(ItemStack stack)` — line 37
- `static PatternSource of(PipeItemsPatternCraftingLogistics pipe)` — line 41
- `static PatternSource heldBy(EntityPlayer player, int inventorySlot)` — line 78

## logisticspipes.crafting.pattern.PatternSource.anonymous@42

Source: `src/main/java/logisticspipes/crafting/pattern/PatternSource.java:42`

- `@Override public int getPatternCount()` — line 44
- `@Override public ItemStack getPatternStack(int slot)` — line 49
- `@Override public void markPatternChanged(int slot)` — line 54
- `@Override public void onSatelliteAssigned(int satelliteId, String satelliteUuid, boolean fluid)` — line 62

## logisticspipes.crafting.pattern.PatternSource.anonymous@79

Source: `src/main/java/logisticspipes/crafting/pattern/PatternSource.java:79`

- `@Override public int getPatternCount()` — line 81
- `@Override public ItemStack getPatternStack(int slot)` — line 86
- `@Override public void markPatternChanged(int slot)` — line 95

## logisticspipes.crafting.pattern.ProcessingPattern

Source: `src/main/java/logisticspipes/crafting/pattern/ProcessingPattern.java:5`

- `public <init>(ItemStack patternStack)` — line 11
- `@Override public int getIngredientSlotCount()` — line 15
- `@Override public int getResultSlotCount()` — line 20

## logisticspipes.crafting.patternStack.IPatternStack

Source: `src/main/java/logisticspipes/crafting/patternStack/IPatternStack.java:7`

- `static IPatternStack fromItemStack(ItemStack stack)` — line 14
- `static IPatternStack readFromNBT(NBTTagCompound tag)` — line 22
- `int getAmount()` — line 35
- `void addAmount(int amount)` — line 37
- `boolean canMerge(IPatternStack other)` — line 39
- `IPatternStack copy()` — line 41
- `ItemStack makePatternStack()` — line 43
- `ItemStack makeDisplayItemStack()` — line 45
- `void writeToNBT(NBTTagCompound tag)` — line 47
- `Item getItem()` — line 49

## logisticspipes.crafting.patternStack.PatternFluidStack

Source: `src/main/java/logisticspipes/crafting/patternStack/PatternFluidStack.java:18`

- `public <init>(FluidIdentifier fluid, int amount)` — line 24
- `public static PatternFluidStack fromItemStack(ItemStack stack)` — line 29
- `public static FluidStack getFluidStack(ItemStack stack, FluidStack fluid)` — line 46
- `public static PatternFluidStack readFromNBT(NBTTagCompound tag)` — line 62
- `public static PatternFluidStack fromFluidStack(FluidStack stack)` — line 70
- `@Override public void addAmount(int amount)` — line 77
- `public FluidStack makeFluidStack()` — line 82
- `public ItemIdentifierStack makeDisplayStack()` — line 86
- `@Override public ItemStack makeDisplayItemStack()` — line 90
- `@Override public ItemStack makePatternStack()` — line 99
- `public NBTTagCompound writeToNBT()` — line 104
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 110
- `@Override public Item getItem()` — line 116
- `@Override public boolean canMerge(IPatternStack other)` — line 121
- `@Override public PatternFluidStack copy()` — line 126
- `@Override public String toString()` — line 131

## logisticspipes.crafting.patternStack.PatternItemStack

Source: `src/main/java/logisticspipes/crafting/patternStack/PatternItemStack.java:9`

- `public <init>(ItemIdentifierStack stack)` — line 19
- `public static PatternItemStack fromItemStack(ItemStack stack)` — line 23
- `public static PatternItemStack readFromNBT(NBTTagCompound tag)` — line 30
- `public static void writeItem(NBTTagCompound tag, ItemIdentifierStack stack)` — line 38
- `public static ItemIdentifierStack readItem(NBTTagCompound tag)` — line 49
- `public ItemIdentifierStack getItemIdentifierStack()` — line 58
- `@Override public int getAmount()` — line 62
- `@Override public void addAmount(int amount)` — line 67
- `@Override public boolean canMerge(IPatternStack other)` — line 72
- `@Override public PatternItemStack copy()` — line 77
- `@Override public ItemStack makePatternStack()` — line 82
- `@Override public ItemStack makeDisplayItemStack()` — line 87
- `@Override public void writeToNBT(NBTTagCompound tag)` — line 92
- `@Override public String toString()` — line 98
- `@Override public Item getItem()` — line 103

## logisticspipes.crafting.patternStack.PatternStackHelper

Source: `src/main/java/logisticspipes/crafting/patternStack/PatternStackHelper.java:10`

- `private <init>()` — line 12
- `public static List<IPatternStack> aggregate(List<? extends IPatternStack> stacks)` — line 14
- `public static void addAggregated(List<IPatternStack> stacks, IPatternStack stack)` — line 31
- `public static IPatternStack copyWithAmount(IPatternStack stack, int amount)` — line 44
- `public static ItemIdentifierStack asSolidStack(IPatternStack stack)` — line 53
- `public static FluidIdentifier asFluid(IPatternStack stack)` — line 60
- `public static boolean isSolid(IPatternStack stack)` — line 67
- `public static boolean isFluid(IPatternStack stack)` — line 71
- `public static boolean containsFluid(List<? extends IPatternStack> stacks)` — line 75
- `public static boolean matches(IPatternStack stack, ItemIdentifier item)` — line 87
- `public static boolean matches(IPatternStack stack, FluidIdentifier fluid)` — line 102
- `public static ItemIdentifier getRoutingItem(IPatternStack stack)` — line 107
- `public static ItemIdentifierStack makeDisplayStack(IPatternStack stack)` — line 116

## logisticspipes.gui.modularUI.pipes.PipeSatelliteMui

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/PipeSatelliteMui.java:20`

- `public <init>(CoreRoutedPipe pipe)` — line 24
- `@Override public String getId()` — line 28
- `@Override public ParentWidget addWidgets(ParentWidget widget, boolean addPlayerInventory)` — line 33
- `@Override public int getWidth()` — line 76
- `@Override public int getHeight()` — line 81

## logisticspipes.gui.modularUI.pipes.patterncrafting.HandheldPatternMui

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/HandheldPatternMui.java:26`

- `public <init>(PlayerInventoryGuiData data)` — line 38
- `public ModularPanel buildUI(PanelSyncManager syncManager, UISettings settings)` — line 42
- `private void lockHeldSlot(PanelSyncManager syncManager)` — line 67
- `private String statusText()` — line 78

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternCraftingContainer

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternCraftingContainer.java:24`

- `public <init>(PatternEditorSyncHandler syncHandler)` — line 28
- `public static Supplier<ModularContainer> supplier(PatternEditorSyncHandler syncHandler)` — line 36
- `@Override public String[] getIdents()` — line 40
- `@Override public int transferRecipe(GuiContainer gui, IRecipeHandler recipe, int recipeIndex, int multiplier)` — line 45
- `@Override public List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainer gui, IRecipeHandler recipe, int recipeIndex)` — line 50
- `@Override public ArrayList<PositionedStack> positionStacks(GuiContainer gui, ArrayList<PositionedStack> stacks)` — line 56

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternCraftingSyncHandler

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternCraftingSyncHandler.java:26`

- `public <init>(PatternEditorState state, PipeItemsPatternCraftingLogistics pipe)` — line 47
- `public PatternCraftingHudState getHudState()` — line 54
- `public PatternCraftingHudState.PatternInfo getSelectedPatternInfo()` — line 58
- `public BlockingMode getBlockingMode()` — line 67
- `public boolean isBlockingModeFixed()` — line 71
- `public ItemStack getTargetStack()` — line 75
- `public ForgeDirection getTargetSide()` — line 79
- `public boolean isPatternUnsupported(int patternSlot)` — line 87
- `public void cancelCraft()` — line 95
- `public void returnInputs()` — line 99
- `public void cycleBlockingMode()` — line 103
- `@Override public void detectAndSendChanges(boolean init)` — line 115
- `@Override public void readOnClient(int id, PacketBuffer buf)` — line 150
- `@Override public void readOnServer(int id, PacketBuffer buf)` — line 167
- `private int computeUnsupportedPatterns()` — line 185
- `private static ItemStack getDisplayStack(World world, int x, int y, int z)` — line 200
- `private static String describe(TileEntity tile)` — line 220

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternEditor

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternEditor.java:46`

- `public <init>(PatternEditorState state, PatternEditorSyncHandler actions, int originX, int originY)` — line 88
- `public PatternEditor inputProgress(IntFunction<String> inputProgress)` — line 98
- `public PatternEditor outputProgress(IntFunction<String> outputProgress)` — line 106
- `public int x(int relativeX)` — line 111
- `public int y(int relativeY)` — line 115
- `public void addTo(ModularPanel panel)` — line 119
- `private void addInputSlot(ModularPanel panel, IItemHandlerModifiable edited, int inputSlot, int x, int y, BooleanSupplier visible)` — line 177
- `private void addOutputSlot(ModularPanel panel, IItemHandlerModifiable edited, int patternSlot, int outputIndex, int x, int y, BooleanSupplier visible)` — line 189
- `private void addEditorButtons(ModularPanel panel)` — line 212
- `ButtonWidget<?> editorButton(int relativeX, int relativeY, int width, int height)` — line 257
- `private IGuiAction.MousePressed patternAction(PatternAction action)` — line 262
- `private IDrawable flagLabel(String key, BooleanSupplier enabled)` — line 270
- `private static void addFlagTooltip(RichTooltip tooltip, String key, boolean enabled)` — line 274
- `private boolean hasEntry(int patternSlot)` — line 284
- `boolean isFluidEntry(int patternSlot)` — line 288
- `private void addInputTooltip(RichTooltip tooltip, int inputSlot)` — line 292
- `private void addEntryHint(RichTooltip tooltip, int patternSlot)` — line 301
- `private void drawSatelliteBadge(int inputSlot)` — line 311
- `private boolean isSatelliteAssigned(int inputSlot, boolean fluid)` — line 328
- `private int getSatelliteId(int inputSlot, boolean fluid)` — line 332
- `private String getSatelliteUuid(int inputSlot, boolean fluid)` — line 343
- `private void addSatelliteTooltip(RichTooltip tooltip, int inputSlot)` — line 354
- `private String satelliteName(int inputSlot, boolean fluid)` — line 374
- `private static String describeLocation(PatternSatelliteInfo satellite)` — line 380
- `private void openSatelliteSelector(int inputSlot)` — line 394
- `private ModularPanel buildSatelliteSelector(ModularPanel parent, EntityPlayer player)` — line 402
- `private void fillSatelliteRows(ModularPanel panel, ListWidget<IWidget, ?> list, StringValue search, int inputSlot, boolean fluid)` — line 433
- `private IWidget satelliteRow(ModularPanel panel, StringValue search, String label, String searchText, boolean current, int inputSlot, int satelliteId, String satelliteUuid, boolean fluid)` — line 478
- `private static boolean matchesSearch(String searchText, String query)` — line 491

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternEditorState

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternEditorState.java:15`

- `public <init>(PatternSource source, int selectedSlot)` — line 23
- `public PatternSource getSource()` — line 29
- `public EditedPatternInventory getEditedInventory()` — line 36
- `public int getSelectedSlot()` — line 40
- `public int getPatternCount()` — line 44
- `public void select(int slot)` — line 48
- `public ItemStack getPatternStack()` — line 53
- `public ItemStack getPatternStack(int slot)` — line 57
- `public boolean hasPattern()` — line 61
- `public AbstractPattern getPattern()` — line 65
- `public boolean isProcessing()` — line 69
- `public void markChanged()` — line 76
- `public static int findInitialSlot(PatternSource source, int preferredSlot)` — line 84
- `public static boolean isPattern(ItemStack stack)` — line 97
- `private int clamp(int slot)` — line 101

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternEditorSyncHandler

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternEditorSyncHandler.java:35`

- `public <init>(PatternEditorState state)` — line 60
- `public PatternEditorState getState()` — line 65
- `public List<PatternSatelliteInfo> getSatellites()` — line 71
- `public int getSatelliteRevision()` — line 78
- `public PatternSatelliteInfo findSatellite(int satelliteId, String satelliteUuid, boolean fluid)` — line 82
- `public void select(int slot)` — line 101
- `public void patternAction(PatternAction action)` — line 106
- `public void assignSatellite(int inputSlot, int satelliteId, String satelliteUuid, boolean fluid)` — line 110
- `public void importRecipe(PatternRecipeImport recipe)` — line 119
- `public void selectMainOutput(int outputSlot)` — line 123
- `public void refreshSatellites()` — line 127
- `@Override public void detectAndSendChanges(boolean init)` — line 133
- `@Override public void readOnClient(int id, PacketBuffer buf)` — line 141
- `@Override public void readOnServer(int id, PacketBuffer buf)` — line 153
- `private void sendSatellites()` — line 181
- `private void applyPatternAction(PatternAction action)` — line 194
- `private static boolean canMultiply(AbstractPattern pattern)` — line 217
- `private static void togglePatternType(ItemStack stack)` — line 233
- `private void applySatellite(int inputSlot, int satelliteId, String satelliteUuid, boolean fluid)` — line 288
- `private void applyImport(PatternRecipeImport recipe)` — line 317
- `private int findBlankPatternSlot()` — line 335

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternEditorSyncHandler.PatternAction

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternEditorSyncHandler.java:47`

No explicit method declarations.

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternGuiDraw

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternGuiDraw.java:11`

- `private <init>()` — line 13
- `static void drawCornerText(String text, int color, boolean top)` — line 18
- `static String formatFluidAmount(int amount)` — line 38
- `static String formatCount(int amount)` — line 54

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternIngredientSlot

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternIngredientSlot.java:26`

- `public PatternIngredientSlot badgeDraw(Runnable draw)` — line 38
- `public PatternIngredientSlot satelliteClick(Runnable action)` — line 43
- `public PatternIngredientSlot mainOutput(Runnable action, BooleanSupplier selected)` — line 48
- `@Override @NotNull public Result onMousePressed(int mouseButton)` — line 54
- `private static ItemStack displayStack(ItemStack stack)` — line 68
- `@Override protected ItemStack getItemStackForRendering(ItemStack stack, boolean dragging)` — line 75
- `@Override public ItemStack getStackForRecipeViewer()` — line 80
- `@Override protected void drawSlotAmountText(int amount, String format)` — line 85
- `public PatternIngredientSlot progressText(Supplier<String> progressText)` — line 93
- `public PatternIngredientSlot extraTooltip(Consumer<RichTooltip> extraTooltip)` — line 98
- `@Override protected void drawOverlay()` — line 103
- `@Override public void buildTooltip(ItemStack stack, RichTooltip tooltip)` — line 122

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternIngredientSlotSH

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternIngredientSlotSH.java:19`

- `public <init>(ModularSlot slot)` — line 23
- `@Override protected void phantomClick(MouseData mouseData, ItemStack cursorStack)` — line 27
- `@Override protected void phantomScroll(MouseData mouseData)` — line 41
- `public static int fluidStep(MouseData mouseData)` — line 54
- `private PatternFluidStack getFluidEntry()` — line 65
- `private void adjustFluid(PatternFluidStack fluid, int delta)` — line 73

## logisticspipes.gui.modularUI.pipes.patterncrafting.PatternSelectSlot

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PatternSelectSlot.java:24`

- `public <init>(int patternSlot, PatternEditorState state, PatternCraftingSyncHandler actions)` — line 34
- `private boolean isSelected()` — line 40
- `@Override @NotNull public Result onMousePressed(int mouseButton)` — line 44
- `@Override public boolean onMouseRelease(int mouseButton)` — line 57
- `@Override public void onMouseDrag(int mouseButton, long timeSinceClick)` — line 66
- `@Override public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme)` — line 73
- `@Override protected void drawOverlay()` — line 86
- `@Override public void buildTooltip(ItemStack stack, RichTooltip tooltip)` — line 97

## logisticspipes.gui.modularUI.pipes.patterncrafting.PipePatternCraftingMui

Source: `src/main/java/logisticspipes/gui/modularUI/pipes/patterncrafting/PipePatternCraftingMui.java:51`

- `public <init>(PipeItemsPatternCraftingLogistics pipe)` — line 69
- `@Override public String getId()` — line 74
- `@Override public int getWidth()` — line 79
- `@Override public int getHeight()` — line 84
- `@Override public ParentWidget addWidgets(ParentWidget widget, boolean addPlayerInventory)` — line 89
- `@Override public ModularPanel getPanel(GuiData guiData, PanelSyncManager syncManager)` — line 95
- `@Override public ModularPanel getPanel(GuiData guiData, PanelSyncManager syncManager, UISettings settings)` — line 100
- `private IWidget buildBlockingModeButton()` — line 132
- `private String modeKey()` — line 147
- `private void addPatternSlots(ModularPanel panel)` — line 151
- `private IWidget buildTargetDisplay(PatternEditor editor)` — line 174
- `private void addPipeButtons(ModularPanel panel, PatternEditor editor)` — line 202
- `private IWidget buildStatusLine()` — line 229
- `private String statusText()` — line 239
- `private String bufferedText(int inputSlot)` — line 261
- `private String requestedText(int outputIndex)` — line 277
- `private static String formatAmount(int amount, boolean fluid)` — line 294

## logisticspipes.gui.popup.PatternRequestMonitorPopup

Source: `src/main/java/logisticspipes/gui/popup/PatternRequestMonitorPopup.java:25`

- `public <init>(PipeBlockRequestTable table, int orderId)` — line 36
- `@Override public void initGui()` — line 43
- `@Override protected void actionPerformed(GuiButton button)` — line 50
- `@Override protected void renderToolTips(int mouseX, int mouseY, float par3)` — line 57
- `@Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)` — line 64
- `@Override protected void renderGuiBackground(int mouseX, int mouseY)` — line 67
- `private int calculateContentHeight(List<PatternCraftingMonitorNode> roots)` — line 100
- `private void renderNode(PatternCraftingMonitorNode node, int centerX, int y, int mouseX, int mouseY)` — line 108
- `private void drawConnection(int parentX, int parentY, int childX, int childY)` — line 124
- `private void renderNodeIcon(PatternCraftingMonitorNode node, int x, int y, int mouseX, int mouseY)` — line 133
- `private void drawScrollHint(int maxScroll)` — line 167

## logisticspipes.modules.ModuleProvider

Source: `src/main/java/logisticspipes/modules/ModuleProvider.java:74`

- `public <init>()` — line 103
- `@Override public void readFromNBT(NBTTagCompound nbttagcompound)` — line 105
- `@Override public void writeToNBT(NBTTagCompound nbttagcompound)` — line 134
- `@Override public ForgeDirection getSneakyDirection()` — line 145
- `@Override public void setSneakyDirection(ForgeDirection sneakyDirection)` — line 150
- `@Override protected ModuleCoordinatesGuiProvider getPipeGuiProvider()` — line 161
- `@Override protected ModuleInHandGuiProvider getInHandGuiProvider()` — line 169
- `protected int neededEnergy()` — line 174
- `protected ItemSendMode itemSendMode()` — line 178
- `protected int itemsToExtract()` — line 182
- `protected int stacksToExtract()` — line 188
- `@Override public SinkReply sinksItem(ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit)` — line 192
- `@Override public LogisticsModule getSubModule(int slot)` — line 198
- `@Override public void tick()` — line 203
- `public boolean filterAllowsItem(ItemIdentifier item)` — line 232
- `@Override public void onBlockRemoval()` — line 240
- `@Override public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters)` — line 247
- `@Override public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info)` — line 279
- `private int getAvailableItemCount(ItemIdentifier item)` — line 290
- `@Override public void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters)` — line 295
- `@Override public void reserveStagedCrafting(ItemIdentifier item, int amount)` — line 335
- `@Override public void releaseStagedCrafting(ItemIdentifier item, int amount)` — line 346
- `private int getReservedStagedCrafting(ItemIdentifier item)` — line 362
- `private int sendStack(ItemIdentifierStack stack, int maxCount, int destination, IAdditionalTargetInformation info)` — line 369
- `private int getTotalItemCount(ItemIdentifier item)` — line 419
- `private boolean hasFilter()` — line 433
- `private boolean itemIsFiltered(ItemIdentifier item)` — line 437
- `@CCCommand(description = "Returns the FilterInventory of this Module") public IInventory getFilterInventory()` — line 442
- `public boolean isExcludeFilter()` — line 447
- `public void setFilterExcluded(boolean isExcludeFilter)` — line 451
- `public boolean isActive()` — line 455
- `public void setIsActive(boolean isActive)` — line 459
- `public ExtractionMode getExtractionMode()` — line 463
- `public void setExtractionMode(int id)` — line 467
- `public void nextExtractionMode()` — line 471
- `@Override public List<String> getClientInformation()` — line 475
- `private void checkUpdate(EntityPlayer player)` — line 486
- `@Override public void startHUDWatching()` — line 513
- `@Override public void stopHUDWatching()` — line 518
- `@Override public void startWatching(EntityPlayer player)` — line 523
- `@Override public void stopWatching(EntityPlayer player)` — line 529
- `@Override public IHUDModuleRenderer getHUDRenderer()` — line 534
- `@Override public void handleInvContent(Collection<ItemIdentifierStack> list)` — line 539
- `public int getExtractionSpeed()` — line 545
- `@Override public boolean hasGenericInterests()` — line 549
- `@Override public List<ItemIdentifier> getSpecificInterests()` — line 554
- `@Override public boolean interestedInAttachedInventory()` — line 567
- `@Override public boolean interestedInUndamagedID()` — line 574
- `@Override public boolean recievePassive()` — line 579
- `@Override @SideOnly(Side.CLIENT) public IIcon getIconTexture(IIconRegister register)` — line 584
- `@Override public LogisticsModularUI getHandGui()` — line 590
- `@Override public LogisticsModularUI getPipeGui()` — line 595
- `@Override public LogisticsModularUI getPipeGui(String prefix)` — line 600

## logisticspipes.nei.PatternRecipeImporter

Source: `src/main/java/logisticspipes/nei/PatternRecipeImporter.java:35`

- `private <init>()` — line 48
- `public static String[] getAllIdents()` — line 56
- `private static void addIdent(Set<String> collected, Object handler)` — line 79
- `public static PatternRecipeImport fromRecipe(IRecipeHandler recipe, int recipeIndex)` — line 88
- `private static PatternRecipeImport tryCraftingGrid(List<PositionedStack> ingredients, List<IPatternStack> outputs)` — line 121
- `private static List<IPatternStack> getOutputs(IRecipeHandler recipe, int recipeIndex, String ident, int limit)` — line 146
- `private static ItemStack firstStack(PositionedStack positioned)` — line 164
- `private static IPatternStack toPatternStack(ItemStack stack)` — line 177

## logisticspipes.network.packets.orderer.PatternCraftingWatchPacket

Source: `src/main/java/logisticspipes/network/packets/orderer/PatternCraftingWatchPacket.java:20`

- `public <init>(int id)` — line 27
- `@Override public void writeData(LPDataOutputStream data)` — line 31
- `@Override public void readData(LPDataInputStream data)` — line 40
- `@Override public void processPacket(EntityPlayer player)` — line 50
- `@Override public ModernPacket template()` — line 58
- `@Override public boolean isCompressable()` — line 63

## logisticspipes.pipes.PipeItemsPatternCraftingLogistics

Source: `src/main/java/logisticspipes/pipes/PipeItemsPatternCraftingLogistics.java:86`

- `public <init>(Item item)` — line 117
- `@Override protected void onAllowedRemoval()` — line 142
- `@Override public void onNeighborBlockChange(int blockId)` — line 147
- `@Override public boolean disconnectPipe(TileEntity tile, ForgeDirection dir)` — line 154
- `@Override protected boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings)` — line 165
- `public AdjacentTile getConnectedInventoryTile()` — line 216
- `public boolean shouldRenderCraftingTargetConnection(ForgeDirection side)` — line 225
- `public boolean isPatternSatelliteLinked(int satelliteId)` — line 230
- `public boolean isPatternSatelliteLinked(String satelliteUuid, int satelliteId)` — line 234
- `public PipeItemsPatternSatelliteLogistics getLinkedPatternSatellite(int satelliteId)` — line 241
- `public boolean hasAdvancedSatelliteUpgrade()` — line 248
- `public PipeItemsPatternSatelliteLogistics getLinkedPatternSatellite(String satelliteUuid, int satelliteId)` — line 252
- `public PipeItemsPatternSatelliteLogistics resolvePatternSatelliteTarget(String satelliteUuid, int satelliteId)` — line 269
- `public PipeFluidPatternSatelliteLogistics resolvePatternFluidSatelliteTarget(String satelliteUuid, int satelliteId)` — line 283
- `public boolean linkPatternSatellite(int satelliteId, String satelliteUuid)` — line 300
- `public boolean linkPatternFluidSatellite(int satelliteId, String satelliteUuid)` — line 309
- `private boolean linkPatternSatellite(int satelliteId, String satelliteUuid, Set<Integer> linkedIds, Set<String> linkedUuids)` — line 317
- `private PipeItemsPatternSatelliteLogistics findPatternSatellite(String satelliteUuid, int satelliteId)` — line 335
- `private PipeFluidPatternSatelliteLogistics findPatternFluidSatellite(String satelliteUuid, int satelliteId)` — line 342
- `private int addLinkedPatternSatellites(List<ItemMemoryChip.StoredPatternSatellite> satellites)` — line 349
- `private int applyLastMemorySatelliteToRecipe(ItemStack memoryChip)` — line 362
- `public void refreshSelectedInventoryConnection()` — line 385
- `@Override public void enabledUpdateEntity()` — line 393
- `@Override public TextureType getCenterTexture()` — line 402
- `@Override public LogisticsModule getLogisticsModule()` — line 407
- `public ModulePatternCrafting getPatternModule()` — line 412
- `public void cancelPatternCraft(int patternSlot)` — line 416
- `public void returnStoredInputsToStorage()` — line 423
- `@Override public boolean canInsertToTanks()` — line 436
- `@Override public boolean canInsertFromSideToTanks()` — line 446
- `@Override public boolean canReceiveFluid()` — line 457
- `@Override public LogisticsFluidOrderManager getFluidOrderManager()` — line 468
- `@Override public LogisticsOrderManager<?, ?> getOrderManager()` — line 479
- `@Override public boolean sharesInterestWith(CoreRoutedPipe other)` — line 490
- `private boolean sharesInventoryInterestWith(CoreRoutedPipe other)` — line 498
- `private List<IInventory> getConnectedInventories(CoreRoutedPipe pipe)` — line 520
- `@Override public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters)` — line 537
- `@Override public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info)` — line 542
- `@Override public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter)` — line 548
- `@Override public void registerExtras(IPromise promise)` — line 553
- `@Override public ICraftingTemplate addCrafting(IResource type)` — line 558
- `@Override public boolean canCraft(IResource toCraft)` — line 563
- `@Override public int getTodo()` — line 568
- `@Override public List<ItemIdentifierStack> getConfiguredCraftResults()` — line 573
- `@Override public Set<ItemIdentifier> getSpecificInterests()` — line 578
- `@Override public double getLoadFactor()` — line 583
- `@Override public int sinkAmount(FluidStack stack)` — line 589
- `@Override public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 594
- `@Override public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info)` — line 599
- `@Override public void listenedChanged()` — line 604
- `private void checkContentUpdate()` — line 610
- `private void checkHudUpdate()` — line 624
- `@Override public void writeToNBT(NBTTagCompound nbttagcompound)` — line 641
- `@Override public void readFromNBT(NBTTagCompound nbttagcompound)` — line 673
- `@Override public void writeData(LPDataOutputStream data)` — line 707
- `@Override public void readData(LPDataInputStream data)` — line 713
- `@Override public void setOrderManagerContent(Collection<ItemIdentifierStack> list)` — line 719
- `public void setHudState(PatternCraftingHudState state)` — line 725
- `public LogisticsFluidOrderManager getPatternFluidOrderManager()` — line 729
- `@Override public IHeadUpDisplayRenderer getRenderer()` — line 733
- `@Override public void startWatching()` — line 738
- `@Override public void stopWatching()` — line 745
- `@Override public void playerStartWatching(EntityPlayer player, int mode)` — line 752
- `@Override public void playerStopWatching(EntityPlayer player, int mode)` — line 771
- `public BlockingMode getBlockingMode()` — line 779
- `public void setBlockingMode(BlockingMode mode)` — line 783
- `public boolean isBlockingModeFixed()` — line 787
- `@Override public LogisticsModularUI getPipeGui()` — line 791
- `@Override public void onWrenchClicked(EntityPlayer entityplayer)` — line 801

## logisticspipes.pipes.PipeItemsPatternCraftingLogistics.BlockingMode

Source: `src/main/java/logisticspipes/pipes/PipeItemsPatternCraftingLogistics.java:90`

No explicit method declarations.

## logisticspipes.pipes.PipeItemsPatternCraftingLogistics.anonymous@118

Source: `src/main/java/logisticspipes/pipes/PipeItemsPatternCraftingLogistics.java:118`

- `@Override public boolean canPipeConnect(TileEntity tile, ForgeDirection dir)` — line 120

## logisticspipes.pipes.PipeItemsProviderLogistics

Source: `src/main/java/logisticspipes/pipes/PipeItemsProviderLogistics.java:78`

- `public <init>(Item item)` — line 99
- `public <init>(Item item, LogisticsItemOrderManager logisticsOrderManager)` — line 105
- `@Override public void onAllowedRemoval()` — line 112
- `public int getTotalItemCount(ItemIdentifier item)` — line 119
- `protected int neededEnergy()` — line 146
- `protected int itemsToExtract()` — line 150
- `protected int stacksToExtract()` — line 154
- `private int sendStack(ItemIdentifierStack stack, int maxCount, int destination, IAdditionalTargetInformation info)` — line 158
- `private IInventoryUtil getAdaptedInventoryUtil(AdjacentTile tile)` — line 219
- `@Override public TextureType getCenterTexture()` — line 251
- `private int getAvailableItemCount(ItemIdentifier item)` — line 256
- `@Override public void enabledUpdateEntity()` — line 263
- `@Override public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters)` — line 304
- `@Override public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info)` — line 355
- `@Override public void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters)` — line 367
- `@Override public void reserveStagedCrafting(ItemIdentifier item, int amount)` — line 421
- `@Override public void releaseStagedCrafting(ItemIdentifier item, int amount)` — line 432
- `private int getReservedStagedCrafting(ItemIdentifier item)` — line 448
- `@Override public LogisticsModule getLogisticsModule()` — line 453
- `@Override public ItemSendMode getItemSendMode()` — line 458
- `@Override public void startWatching()` — line 463
- `@Override public void stopWatching()` — line 470
- `private void updateInv(EntityPlayer player)` — line 477
- `@Override public void listenedChanged()` — line 504
- `private void checkContentUpdate(EntityPlayer player)` — line 509
- `@Override public void playerStartWatching(EntityPlayer player, int mode)` — line 527
- `@Override public void playerStopWatching(EntityPlayer player, int mode)` — line 538
- `@Override public void setReceivedChestContent(Collection<ItemIdentifierStack> list)` — line 544
- `@Override public IHeadUpDisplayRenderer getRenderer()` — line 551
- `@Override public void setOrderManagerContent(Collection<ItemIdentifierStack> list)` — line 556
- `@Override public Set<ItemIdentifier> getSpecificInterests()` — line 562
- `@Override public double getLoadFactor()` — line 586
- `public ItemIdentifierInventory getprovidingInventory()` — line 592
- `@Override public void readFromNBT(NBTTagCompound nbttagcompound)` — line 596
- `@Override public void writeToNBT(NBTTagCompound nbttagcompound)` — line 602
- `public boolean hasFilter()` — line 611
- `public boolean itemIsFiltered(ItemIdentifier item)` — line 615
- `public boolean isExcludeFilter()` — line 619
- `@Deprecated public void setFilterExcluded(boolean isExcluded)` — line 623
- `public ExtractionMode getExtractionMode()` — line 631
- `@Deprecated public void setExtractionMode(int id)` — line 635
- `@Deprecated public void nextExtractionMode()` — line 643
- `@Override public LogisticsModularUI getPipeGui()` — line 651

## logisticspipes.pipes.upgrades.CraftingMonitoringUpgrade

Source: `src/main/java/logisticspipes/pipes/upgrades/CraftingMonitoringUpgrade.java:7`

- `@Override public boolean needsUpdate()` — line 9
- `@Override public boolean isAllowedForPipe(CoreRoutedPipe pipe)` — line 14
- `@Override public boolean isAllowedForModule(LogisticsModule pipe)` — line 19
- `@Override public String[] getAllowedPipes()` — line 24
- `@Override public String[] getAllowedModules()` — line 29

## logisticspipes.pipes.upgrades.InstantSatelliteUpgrade

Source: `src/main/java/logisticspipes/pipes/upgrades/InstantSatelliteUpgrade.java:8`

- `@Override public boolean needsUpdate()` — line 10
- `@Override public boolean isAllowedForPipe(CoreRoutedPipe pipe)` — line 15
- `@Override public boolean isAllowedForModule(LogisticsModule module)` — line 20
- `@Override public String[] getAllowedPipes()` — line 25
- `@Override public String[] getAllowedModules()` — line 30

## logisticspipes.pipes.upgrades.PatternUpgrade

Source: `src/main/java/logisticspipes/pipes/upgrades/PatternUpgrade.java:8`

- `@Override public boolean needsUpdate()` — line 10
- `@Override public boolean isAllowedForPipe(CoreRoutedPipe pipe)` — line 15
- `@Override public boolean isAllowedForModule(LogisticsModule pipe)` — line 20
- `@Override public String[] getAllowedPipes()` — line 25
- `@Override public String[] getAllowedModules()` — line 30

## logisticspipes.renderer.PatternItemRenderer

Source: `src/main/java/logisticspipes/renderer/PatternItemRenderer.java:15`

- `public static void setForceResultRender(boolean forceResultRender)` — line 21
- `public static void clearForceResultRender()` — line 25
- `@Override public boolean handleRenderType(ItemStack itemStack, ItemRenderType rendererType)` — line 29
- `@Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)` — line 41
- `@Override public void renderItem(ItemRenderType renderType, ItemStack itemStack, Object... data)` — line 46
- `private boolean isForceResultRender()` — line 61
- `private boolean isShiftPressed()` — line 66

## logisticspipes.request.RequestTree

Source: `src/main/java/logisticspipes/request/RequestTree.java:29`

- `public <init>(IResource requestType, RequestTree parent, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info)` — line 44
- `private int getExistingPromisesFor(FinalPair<IProvide, ItemIdentifier> key)` — line 49
- `public int getAllPromissesFor(IProvide provider, ItemIdentifier item)` — line 60
- `public LinkedList<IExtraPromise> getExtrasFor(IResource item)` — line 65
- `protected LinkedLogisticsOrderList fullFillAll()` — line 76
- `public void sendMissingMessage(RequestLog log)` — line 80
- `public void sendUsedMessage(RequestLog log)` — line 86
- `protected void promiseAdded(IPromise promise)` — line 94
- `protected void promiseRemoved(IPromise promise)` — line 102
- `public static boolean request(List<ItemIdentifierStack> items, IRequestItems requester, RequestLog log, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info)` — line 150
- `public static int request(ItemIdentifierStack item, IRequestItems requester, RequestLog log, boolean acceptPartial, boolean simulateOnly, boolean logMissing, boolean logUsed, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info)` — line 189
- `public static boolean request(ItemIdentifierStack item, IRequestItems requester, RequestLog log, IAdditionalTargetInformation info)` — line 228
- `public static int requestPartial(ItemIdentifierStack item, IRequestItems requester, IAdditionalTargetInformation info)` — line 235
- `public static int simulate(ItemIdentifierStack item, IRequestItems requester, RequestLog log)` — line 241
- `public static int requestFluidPartial(FluidIdentifier liquid, int amount, IRequestFluid pipe, RequestLog log)` — line 246
- `public static int requestFluidPartial(FluidIdentifier liquid, int amount, IRequestFluid pipe, RequestLog log, IAdditionalTargetInformation info)` — line 250
- `public static boolean requestFluid(FluidIdentifier liquid, int amount, IRequestFluid pipe, RequestLog log)` — line 255
- `private static int requestFluid(FluidIdentifier liquid, int amount, IRequestFluid pipe, RequestLog log, boolean acceptPartial, IAdditionalTargetInformation info)` — line 259

## logisticspipes.request.RequestTree.ActiveRequestType

Source: `src/main/java/logisticspipes/request/RequestTree.java:31`

No explicit method declarations.

## logisticspipes.request.RequestTree.workWeightedSorter

Source: `src/main/java/logisticspipes/request/RequestTree.java:112`

- `public <init>(double distanceWeight)` — line 116
- `@Override public int compare(ExitRoute o1, ExitRoute o2)` — line 120

## logisticspipes.request.RequestTreeNode

Source: `src/main/java/logisticspipes/request/RequestTreeNode.java:48`

- `protected <init>(IResource requestType, RequestTreeNode parentNode, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info)` — line 50
- `private <init>(ICraftingTemplate template, IResource requestType, RequestTreeNode parentNode, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info)` — line 55
- `private boolean isCrafterUsed(ICraftingTemplate test)` — line 104
- `private boolean declareCrafterUsed(ICraftingTemplate test)` — line 115
- `public int getPromiseAmount()` — line 123
- `public int getMissingAmount()` — line 127
- `public void addPromise(IPromise promise)` — line 131
- `public boolean isDone()` — line 160
- `public boolean isAllDone()` — line 164
- `protected void remove(RequestTreeNode subNode)` — line 172
- `protected void removeSubPromisses()` — line 178
- `public int getPromisedByproductAmount(IProvide provider, ItemIdentifier item, PatternByproductTarget target)` — line 188
- `protected void checkForExtras(IResource item, HashMap<IProvide, List<IExtraPromise>> extraMap)` — line 205
- `protected void removeUsedExtras(IResource item, HashMap<IProvide, List<IExtraPromise>> extraMap)` — line 217
- `protected LinkedLogisticsOrderList fullFill()` — line 249
- `private boolean hasStagedCraftingPromise()` — line 275
- `private boolean isStagedCraftingPromise(IPromise promise)` — line 287
- `private LinkedLogisticsOrderList fullFillStaged()` — line 297
- `private PatternCraftingBranch toPatternCraftingBranch()` — line 341
- `private List<IExtraPromise> copyExtraPromises(List<IExtraPromise> promises)` — line 365
- `protected void buildMissingMap(Map<IResource, Integer> missing)` — line 373
- `protected void buildUsedMap(Map<IResource, Integer> used, Map<IResource, Integer> missing)` — line 387
- `private boolean checkProvider()` — line 415
- `private void beginSameItemPromiseCollection()` — line 438
- `private void finishSameItemPromiseCollection()` — line 446
- `private void collectSameItemPromise(IPromise promise)` — line 470
- `private boolean isSameItemDictRequest()` — line 477
- `private boolean acceptSameItemPromise(IPromise promise)` — line 481
- `private int totalPromiseAmount(List<IPromise> promises)` — line 489
- `private static List<ExitRoute> routesToInterestedRouters(IRouter source, BitSet routersIndex)` — line 501
- `private static List<Pair<IProvide, List<IFilter>>> getProviders(IRouter destination, IResource item)` — line 526
- `private boolean checkExtras()` — line 547
- `private boolean checkCrafting()` — line 579
- `public boolean hasBeenQueried(LogisticsOrderManager<?, ?> orderManager)` — line 712
- `public void setQueried(LogisticsOrderManager<?, ?> orderManager)` — line 716
- `private static List<Pair<ICraftingTemplate, List<IFilter>>> getCrafters(IResource iRequestType, List<ExitRoute> validDestinations)` — line 799
- `private int getSubRequests(int nCraftingSets, ICraftingTemplate template)` — line 823
- `private int generateRequestTreeFor(int workSets, ICraftingTemplate template)` — line 860
- `void recurseFailedRequestTree()` — line 890
- `protected void logFailedRequestTree(RequestLog log)` — line 917
- `private void destroy()` — line 930
- `protected static List<IResource> shrinkToList(Map<IResource, Integer> items)` — line 934
- `@Override public String toString()` — line 947
- `private void appendToString(StringBuilder out, String prefix, boolean isLast, java.util.Set<RequestTreeNode> visited)` — line 956
- `private String formatPromises(java.util.Collection<? extends IPromise> promises, boolean includeType)` — line 1016

## logisticspipes.request.RequestTreeNode.CraftingSorterNode

Source: `src/main/java/logisticspipes/request/RequestTreeNode.java:720`

- `<init>(Pair<ICraftingTemplate, List<IFilter>> crafter, int maxCount, RequestTree tree, RequestTreeNode treeNode)` — line 730
- `int calculateMaxWork(int maxSetsToCraft)` — line 740
- `int addToWorkRequest(int extraWork)` — line 758
- `boolean addWorkPromisesToTree()` — line 767
- `@Override public int compareTo(CraftingSorterNode o)` — line 789
- `public int currentToDo()` — line 794

## logisticspipes.request.debug.CraftingRequestDebugManager

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:23`

- `private <init>()` — line 48
- `public static void record(String title, RequestTree tree, LinkedLogisticsOrderList orders)` — line 53
- `public static void recordEvent(String category, String message)` — line 73
- `public static void recordPipeEvent(PipeItemsPatternCraftingLogistics pipe, String category, String message)` — line 88
- `public static String buildSnapshot()` — line 95
- `private static void appendSummary(StringBuilder out)` — line 108
- `private static void appendCraftingFlow(StringBuilder out)` — line 130
- `private static void appendTimeline(StringBuilder out)` — line 140
- `private static void appendVerboseTimeline(StringBuilder out)` — line 161
- `private static List<DebugEvent> eventsOldestFirst()` — line 174
- `private static void appendEventLine(StringBuilder out, DebugEvent event)` — line 183
- `private static boolean isCompactTimelineEvent(DebugEvent event)` — line 188
- `private static void appendRecordedRequests(StringBuilder out)` — line 206
- `private static int countActivePatternPipes()` — line 229
- `private static void appendActivePatternPipes(StringBuilder out)` — line 245
- `private static String formatOrderList(LinkedLogisticsOrderList orders)` — line 275
- `private static void appendOrderList(StringBuilder out, LinkedLogisticsOrderList orders, String prefix, String label)` — line 284
- `private static void appendOrder(StringBuilder out, IOrderInfoProvider order, String prefix)` — line 297
- `private static String summarizeOrderList(LinkedLogisticsOrderList orders)` — line 313
- `private static String describePipe(PipeItemsPatternCraftingLogistics pipe)` — line 321
- `private static String formatTime(long time)` — line 332
- `public static void clear()` — line 336
- `private static PipeMessage splitPipeMessage(String message)` — line 346
- `private static int parseInt(String value, int fallback)` — line 354
- `private static int parseTargetSlot(String info)` — line 362
- `private static String simplifyTarget(String target)` — line 367
- `private static String formatBriefEvent(DebugEvent event, String body)` — line 391

## logisticspipes.request.debug.CraftingRequestDebugManager.CraftingFlowBuilder

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:395`

- `private void accept(DebugEvent event)` — line 402
- `private boolean acceptStagedEvent(DebugEvent event, PipeMessage pipeMessage, String message)` — line 429
- `private boolean acceptOrderEvent(PipeMessage pipeMessage, String message)` — line 463
- `private boolean acceptRequestEvent(PipeMessage pipeMessage, String message)` — line 476
- `private FlowCraft findLastPendingCraft(String pipeKey)` — line 521
- `private FlowCraft findLatestCraftBySlot(String pipeKey, int patternSlot)` — line 531
- `private boolean samePipe(String left, String right)` — line 542
- `private void append(StringBuilder out)` — line 549

## logisticspipes.request.debug.CraftingRequestDebugManager.FlowCraft

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:574`

- `private <init>(String pipeKey, int tick, String output, int amount, int parentSlot)` — line 590
- `private void append(StringBuilder out, String prefix)` — line 598
- `private void appendSetSummary(StringBuilder out, String prefix)` — line 620
- `private void appendIngredients(StringBuilder out, String prefix)` — line 633

## logisticspipes.request.debug.CraftingRequestDebugManager.FlowSetRequest

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:656`

- `private <init>(int remainingSets, int orderableSets, int branchSets, int selectedSets)` — line 663

## logisticspipes.request.debug.CraftingRequestDebugManager.FlowIngredientRequest

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:671`

- `private <init>(String ingredient, String target, int requested, int amountPerSet)` — line 678
- `private FlowIngredientRequest copy()` — line 685

## logisticspipes.request.debug.CraftingRequestDebugManager.PipeMessage

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:690`

- `private <init>(String pipeKey, String body)` — line 695
- `private String messageWithPipe()` — line 700

## logisticspipes.request.debug.CraftingRequestDebugManager.RequestSnapshot

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:705`

- `private <init>(int id, String title, long time, String treeText, String ordersText)` — line 713

## logisticspipes.request.debug.CraftingRequestDebugManager.DebugEvent

Source: `src/main/java/logisticspipes/request/debug/CraftingRequestDebugManager.java:722`

- `private <init>(int id, long time, int tick, String category, String message)` — line 730

## logisticspipes.routing.ItemRoutingInformation

Source: `src/main/java/logisticspipes/routing/ItemRoutingInformation.java:21`

- `@Override public ItemRoutingInformation clone()` — line 40
- `public void readFromNBT(NBTTagCompound nbttagcompound)` — line 72
- `public void writeToNBT(NBTTagCompound nbttagcompound)` — line 86
- `private IAdditionalTargetInformation readTargetInfo(NBTTagCompound tag)` — line 103
- `private NBTTagCompound writeTargetInfo(IAdditionalTargetInformation info)` — line 116
- `public long getTimeOut()` — line 133
- `public long getTickToTimeOut()` — line 138
- `public void resetDelay()` — line 142
- `@Override public String toString()` — line 149

## logisticspipes.routing.ItemRoutingInformation.DelayComparator

Source: `src/main/java/logisticspipes/routing/ItemRoutingInformation.java:31`

- `@Override public int compare(ItemRoutingInformation o1, ItemRoutingInformation o2)` — line 33
