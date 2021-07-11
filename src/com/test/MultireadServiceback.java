package com.test;



import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.jcp.common.config.PropertiesConfig;
import com.jcp.common.defaults.Constants;
import com.jcp.common.defaults.Util;
import com.jcp.common.models.*;
import com.jcp.common.models.enums.PriceType;
import com.jcp.common.models.exclusion.ExclusionContainer;
import com.jcp.common.models.exclusion.ExclusionLocations;
import com.jcp.common.models.response.MultiReadResponse;
import com.jcp.common.models.response.MultiReadResponseList;
import com.jcp.common.sterling.models.ItemDetails;
import com.jcp.common.sterling.models.NodeCapacity;
import com.jcp.common.util.*;
import com.jcp.omni.inventory.application.ConfigurationsEureka;
import com.jcp.omni.inventory.config.ITopicEvent;
import com.jcp.omni.inventory.wrapper.enums.NodeType;
import com.jcpenney.isp.inventory.datagrid.message.plugin.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jcp.common.defaults.Constants.*;
import static com.jcp.common.util.ItemInventoryUtil.getNetworkQualitativeStatus;
import static com.jcp.omni.inventory.application.ConfigurationsEureka.*;
import static com.jcp.omni.inventory.utils.EventUtils.getBoolean;
import static com.jcp.omni.inventory.utils.ServiceUtils.getExclusionProperties;
import static com.jcp.omni.inventory.utils.ServiceUtils.getNodeIndex;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component

public class MultireadServiceback {

    @Autowired
    private ItemCurrentPriceUtil itemCurrentPriceUtil;

    @Autowired
    private ITopicEvent event;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private FeatureLevelExclusionControlUtils featureLevelExclusionControlUtils;

    @Autowired
    private PropertiesConfig propertiesConfig;

    public static final String LOCATION_DATA_NOT_AVAILABLE = "Location or NodeType is not available";

    /**
     * Method to return true if whse node
     * @param svcResponse
     * @return
     */
    public Boolean checkIfWhseLocation(MultiReadResponse svcResponse) {
        return !(svcResponse.getNodeType().equals(LOCATION_DATA_NOT_AVAILABLE)
                || svcResponse.getNodeType().equals(Constants.STORE));
    }

    /**
     * Method to return true if whse node AND isWhseAtpUpdate flag is TRUE in request of MR
     * @param itemId
     * @param multiReadResponse
     * @param itemsForWhseAtpUpdate
     * @return
     */
    public Boolean checkIfWhseAtpUpdateRequired(String itemId, MultiReadResponse multiReadResponse,
                                                Map<String, Boolean> itemsForWhseAtpUpdate) {
        return !(multiReadResponse.getNodeType().equals(LOCATION_DATA_NOT_AVAILABLE)
                || multiReadResponse.getNodeType().equals(Constants.STORE)) && Objects.nonNull(itemsForWhseAtpUpdate.get(itemId));
    }

    /*
   Method to calculate the grossAtp for summing up inventory across all the stores & updating
   as part of purify-atp
   In the scenario of a node having dirty key or markdown exclusion, atp is assumed as 0
  */
    public int getGrossAtp(int supply, int demand, Map<String, Integer> inventoryValueForOutput, Boolean isNodeDirty,boolean isExcluded, boolean isShip) {
        log.debug("Node is dirty? {}", isNodeDirty);
        if (Boolean.TRUE.equals(isNodeDirty) || isExcluded) {
            return 0;
        } else {
            if(isShip)
                return ((supply - demand - inventoryValueForOutput.get("shipSafetyFactor")) < 0 ? 0 : (supply - demand - inventoryValueForOutput.get("shipSafetyFactor")));
            else
                return ((supply - demand - inventoryValueForOutput.get("bopusSafetyFactor")) < 0 ? 0 : (supply - demand - inventoryValueForOutput.get("bopusSafetyFactor")));
        }
    }


    /**
     * This method updates the ship and bopus safety factor in the multiread output
     * @param inventoryValueForOutput
     * @param safetyFactorDetails
     * @param itemdetails
     */
    public void setSafetyFactorInMROutput(Map<String, Integer> inventoryValueForOutput,  Map<String, SafetyStock> safetyFactorDetails, ItemDetails itemdetails){
        int shipsf = 0;
        int bopussf = 0;
        SafetyStock safetyFactor = safetyFactorDetails.get(
                itemdetails.getPurchaseType() + Constants.SEPARATOR + itemdetails.getProductLine());
        if (getBoolean(DYNAMIC_SAFETY_FACTOR_ENABLED)) {
            shipsf = Optional.ofNullable(inventoryValueForOutput.get(SHIP_SAFETY_FACTOR))
                    .orElse(EventUtils.getShipSF(safetyFactor));
            bopussf = Optional.ofNullable(inventoryValueForOutput.get(BOPUS_SAFETY_FACTOR))
                    .orElse(EventUtils.getPickSF(safetyFactor));
        } else {
            shipsf = EventUtils.getShipSF(safetyFactor, itemdetails);
            bopussf = EventUtils.getPickSF(safetyFactor, itemdetails);
        }
        inventoryValueForOutput.put(SHIP_SAFETY_FACTOR, shipsf);
        inventoryValueForOutput.put(BOPUS_SAFETY_FACTOR, bopussf);
    }

    /**
     * This method returns if SFS Exclusion is applied for a particular item
     * @param itemID
     * @param asyncResults
     * @param storeID
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public boolean isSFSExclusionsApplied(String itemID, YihAsyncResultsHandler asyncResults, String storeID)
            throws ExecutionException, InterruptedException{
        BitSet markDownStatus = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID.split("\\|")[1])))
                .orElse(ItemInventory.builder().markDownStatus(new BitSet()).build()).getMarkDownStatus();
        markDownStatus = Optional.ofNullable(markDownStatus).orElse(new BitSet());

        BitSet sfsExclusion = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID.split("\\|")[1])))
                .orElse(ItemInventory.builder().sfsExclusion(new BitSet()).build()).getSfsExclusion();
        sfsExclusion = Optional.ofNullable(sfsExclusion).orElse(new BitSet());

        return !(Util.isStoreActive(markDownStatus, sfsExclusion, getNodeIndex(asyncResults.getLocationIndexFuture().get(),storeID.split("\\|")[1])));
    }

    /**
     * This method returns if BOPUS Exclusion is present for a particular item
     * @param itemID
     * @param asyncResults
     * @param storeID
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public boolean isBopusExclusionsApplied(String itemID, YihAsyncResultsHandler asyncResults, String storeID)
            throws ExecutionException, InterruptedException{
        BitSet markDownStatus = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID.split("\\|")[1])))
                .orElse(ItemInventory.builder().markDownStatus(new BitSet()).build()).getMarkDownStatus();
        markDownStatus = Optional.ofNullable(markDownStatus).orElse(new BitSet());

        BitSet bopusExclusion = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID.split("\\|")[1])))
                .orElse(ItemInventory.builder().bopusExclusion(new BitSet()).build()).getBopusExclusion();
        bopusExclusion = Optional.ofNullable(bopusExclusion).orElse(new BitSet());
        return !(Util.isStoreActive(markDownStatus, bopusExclusion, getNodeIndex(asyncResults.getLocationIndexFuture().get(),storeID.split("\\|")[1])));
    }

    /**
     * This method is for updatingWHSEATPBlob
     * @param itemsActiveWhseATP
     * @param supply
     * @param demand
     * @param inventoryValueForOutput
     * @param inventoryKeySplit
     * @param isWhseAtpAddedMap
     * @param nodeIndexMap
     */
    public void updateWhseATPBlob(Map<String, int[]> itemsActiveWhseATP, int supply, int demand, Map<String, Integer> inventoryValueForOutput,
                                  String[] inventoryKeySplit, Map<String, Boolean> isWhseAtpAddedMap, Map<String,Integer> nodeIndexMap) {

        int grossWhseAtp = (supply - demand) < 0 ? 0 : (supply - demand);
        inventoryValueForOutput.put(ServiceConstants.WHSE_ATP, grossWhseAtp);
        inventoryValueForOutput.put("grossWhseAtp", grossWhseAtp);
        if (itemsActiveWhseATP != null && itemsActiveWhseATP
                .containsKey(inventoryKeySplit[2])) {
            int[] itemActiveWhseATP = itemsActiveWhseATP.get(inventoryKeySplit[2]);
            if (getNodeIndex(nodeIndexMap, inventoryKeySplit[1]) != -1) {
                isWhseAtpAddedMap.put(inventoryKeySplit[2], Boolean.TRUE);
                itemActiveWhseATP[getNodeIndex(nodeIndexMap,  inventoryKeySplit[1])] = grossWhseAtp;
            } else {
                log.debug("NodeId {} not found in the jcp_whse_index map", inventoryKeySplit[1]);
            }
            itemsActiveWhseATP.put(inventoryKeySplit[2], itemActiveWhseATP);
        }
    }

    /**
     * Used to differentiate between Purify atp request and multi re
     * @param query
     * @return
     */
    public boolean isPurifyAtpRequest(InventoryQuerry query) {
        return query.getIsUpdateMode() != null;
    }

    /**
     * Method is used to fetch all stores list from jcp_distribution_group keys in form of JCP|storeid
     * based and request
     * @return set of nodes.
     */
    public Set<String> fetchStoreFromDG(List<InventoryQuerry> inventorydata,
                                        List<MultiReadResponse> multiReadResponseList, Map<Integer, List<String>> storeDGMap,
                                        Map<Integer, List<String>> whseDGMap, int size, Map<String, Boolean> mapForValues) {
        //nodes will hold keys in form of JCP|storeid
        Set<String> nodes = new HashSet<>();
        Set<String> nodesAdded = new HashSet<>();
        Boolean isPurifyAtpRequest = mapForValues.get("isPurifyAtpRequest");
        if(inventorydata.size() > 0) {
            isPurifyAtpRequest = isPurifyAtpRequest(inventorydata.get(0));
        }

        boolean finalIsPurifyAtpRequest = isPurifyAtpRequest;
        inventorydata.stream().forEach(query -> {
            /**
             * If size of nodesAdded set is equal to the size of distributionGroupMap
             * means all the stores and whse are added in the node set.
             */
            if (nodesAdded.size() < size) {
                /**
                 * If the call is coming from Purify ATP API then
                 * we will have either of the updateStoreAtp or updateWhseAtp in request
                 * Else the call is from Multi Read API
                 */
                if (finalIsPurifyAtpRequest) {
                    if (query.isUpdateStoreAtps() && !nodesAdded.contains(Constants.JCP_SFS_DG)) {
                        nodes.addAll(storeDGMap.get(1));
                        nodesAdded.add(Constants.JCP_SFS_DG);
                    }
                    if (query .isUpdateWhseAtp() && !nodesAdded.contains(Constants.JCP_WHSE_DG)) {
                        nodes.addAll(whseDGMap.get(1));
                        nodesAdded.add(Constants.JCP_WHSE_DG);
                    }
                } else {
                    if (!StringUtils.isBlank(query.getStoreID())) {
                        nodes.add("JCP|" + query.getStoreID());
                        if(Boolean.parseBoolean(event.getProperties().get(ConfigurationsEureka.FEATURE_SUBSTITUTE_AVAILABILITY))){
                            Set<String> itemIDToCheckIfDLC = new HashSet<>();
                            itemIDToCheckIfDLC.add("JCP|" +query.getStoreID());
                            Map<String, Location> locationMap = Util.getAllFromHazelcast(hazelcastInstance, Constants.JCP_LOCATION, itemIDToCheckIfDLC);
                            if (!locationMap.get("JCP|" +query.getStoreID()).getNodeType().equals(NodeType.Store.name())){
                                List<String> whseStores = getStoresFromDg(Constants.JCP_WHSE_DG, new HashSet<>(), null, whseDGMap);
                                nodes.addAll(whseStores);
                            }
                        }
                    } else {
                        assignDGAndNodeType.apply(query);
                        MultiReadResponse svcResponse = validateDGandNodeType(query);
                        if (svcResponse.getStatus() != null && svcResponse.getStatus().equals(BAD_REQUEST)) {
                            multiReadResponseList.add(svcResponse);
                        } else {
                            nodes.addAll( getStoresFromDg(query.getDistributionGroup(),nodesAdded, storeDGMap, whseDGMap));
                        }
                    }
                }
            }
        });
        mapForValues.put("isPurifyAtpRequest", isPurifyAtpRequest);
        return nodes;
    }

    /**
     * Validate method to validate distributionGroup
     * and nodeType request attributes
     * @param inventoryQuerry
     * @return SvcResponse
     */
    public MultiReadResponse validateDGandNodeType(InventoryQuerry inventoryQuerry) {
        MultiReadResponse response =  new MultiReadResponse();
        if (!inventoryQuerry.getNodeType().equals(ALL) &&
                !EnumUtils.isValidEnum(NodeType.class, inventoryQuerry.getNodeType())) {
            response.setMessage("NodeType must be either one of Store/DC/LC/RC"+" (Reference Item Id "+ inventoryQuerry.getItemID()+")");
            response.setStatus(BAD_REQUEST);
        } else if (inventoryQuerry.getDistributionGroup().equals(JCP_SFS_DG) &&
                (!NodeType.Store.name().equals(inventoryQuerry.getNodeType()) &&
                        !inventoryQuerry.getNodeType().equals(ALL))) {
            response.setMessage("For Distribution Group " + JCP_DG + " NodeType must be " + NodeType.Store.name()+" (Reference Item Id "+ inventoryQuerry.getItemID()+")");
            response.setStatus(BAD_REQUEST);
        } else if (inventoryQuerry.getDistributionGroup().equals(JCP_WHSE_DG) &&
                NodeType.Store.name().equals(inventoryQuerry.getNodeType())) {
            response.setMessage("For Distribution Group " + JCP_WHSE_DG + " NodeType must be either one of DC/LC/RC"+" (Reference Item Id "+ inventoryQuerry.getItemID()+")");
            response.setStatus(BAD_REQUEST);
        }
        return response;
    }

    /**
     * Method to get storesList from JCP_DISTRIBUTION_GROUP map based on distribution group sent form request.
     * If param jcpDg = ALL --> add stores of both SFS and WHSE
     * If param jcpDg = a particular SFS DG or WHSE DG, then return stores of that particular DG
     * @param jcpDg
     * @param storeDGMap
     *@param whseDGMap @return  List<String>
     */
    public List<String> getStoresFromDg(String jcpDg, Set<String> nodesAdded, Map<Integer, List<String>> storeDGMap, Map<Integer, List<String>> whseDGMap) {
        List<String> stores = new ArrayList<>();
        if (jcpDg.equals(ALL)) {
            stores.addAll(storeDGMap.get(1));
            stores.addAll(whseDGMap.get(1));
            nodesAdded.add(Constants.JCP_WHSE_DG);
            nodesAdded.add(Constants.JCP_SFS_DG);
        } else {
            Map<Integer, List<String>> dgList;
            dgList = Constants.JCP_SFS_DG.equals(jcpDg) ? storeDGMap : whseDGMap;
            if (dgList != null) {
                stores.addAll(dgList.get(1));
            }
            nodesAdded.add(jcpDg);
        }
        return stores;
    }

    /**
     * This method updates the exclusions and price exclusion fields in the item inventory table
     * @param updatedExclusions
     * @param itemID
     * @param inventoryValue

     * @param currPrice
     */
    public void insertFeatureExclusionsInItemInv  (Map<String, Boolean> updatedExclusions, String itemID, ItemInventory inventoryValue,
                                                   Map<ItemPriceKey, ItemPrice> currPrice, PropertiesConfig propertiesConfig)
    {
        if(null != inventoryValue) {
            inventoryValue.setSubLotSFSExclusion(updatedExclusions.get("subLotSFSExclusion"));
            inventoryValue.setSubLotBopusExclusion(updatedExclusions.get("subLotBopusExclusion"));
            inventoryValue.setPerformanceSFSExclusion(updatedExclusions.get("performanceSFSExclusion"));
            inventoryValue.setPerformanceBopusExclusion(updatedExclusions.get("performanceBOPUSExclusion"));
            ItemPrice itemPrice = currPrice.get(new ItemPriceKey(itemID));
            inventoryValue.setBelowaur(
                    itemCurrentPriceUtil.hasAURBasedExclusion(itemPrice, propertiesConfig, true) || itemCurrentPriceUtil.hasAURBasedExclusion(itemPrice, propertiesConfig, false));
            inventoryValue.setOnclearance(
                    itemCurrentPriceUtil.hasClearanceBasedExclusion(itemPrice, propertiesConfig, true) || itemCurrentPriceUtil.hasClearanceBasedExclusion(itemPrice, propertiesConfig, false));
        }
        if (inventoryValue.isSubLotSFSExclusion() || inventoryValue.isPerformanceSFSExclusion()) {
            inventoryValue.setShipActive(0);
        }

        if (inventoryValue.isSubLotBopusExclusion() || inventoryValue.isPerformanceBopusExclusion()) {
            inventoryValue.setBopusActive(0);
        }
    }

    public Function<InventoryQuerry,InventoryQuerry> assignDGAndNodeType = (inventoryQuerry) -> {
        if(StringUtils.isBlank(inventoryQuerry.getDistributionGroup())){
            inventoryQuerry.setDistributionGroup(JCP_SFS_DG);
        }
        if (StringUtils.isBlank(inventoryQuerry.getNodeType())) {
            inventoryQuerry.setNodeType(ALL);
        }
        return inventoryQuerry;
    };

    //Method added for 8657 which checks if exclusion is present for product line, sublot and performance
    public void checkForExclusions(String itemID, IMap<String, ExclusionContainer> exclusionMap, Map<String, Boolean> exclusionCalcMap) {
        ExclusionContainer exclusionContainer = exclusionMap.get(itemID.substring(0,3));
        if (exclusionContainer.getExclusions().entrySet() != null)
            exclusionCalcMap.put("ProductLineExcl", true);
        else
            exclusionCalcMap.put("ProductLineExcl", false);

        if (exclusionContainer.getExclusions().get(itemID) != null)
            exclusionCalcMap.put("PerformanceExclusion", true);
        else
            exclusionCalcMap.put("PerformanceExclusion", false);

        if(exclusionContainer.getExclusions().get(itemID.substring(0, 7)) != null || (exclusionContainer.getExclusions().get(itemID.substring(0, 3)) != null))
            exclusionCalcMap.put("SublotExclusion", true);
        else
            exclusionCalcMap.put("SublotExclusion", false);
    }

    public boolean isExcludedAcrossStoresForPick(String itemId,
                                                 Map<String, ExclusionLocations> newExclusionss) {
        return isExcludedAcrossStores(itemId, false, newExclusionss);
    }

    public boolean isExcludedAcrossStoresForShip(String itemId,
                                                 Map<String, ExclusionLocations> newExclusionss) {
        return isExcludedAcrossStores(itemId, true, newExclusionss);
    }

    private boolean isExcludedAcrossStores(String itemId, boolean excludedForShip,
                                           Map<String, ExclusionLocations> newExclusionss) {
        boolean isExcluded = false;
        long startTime = System.nanoTime();
        if (newExclusionss != null && (newExclusionss.get(itemId) != null && newExclusionss
                .get(itemId).isShipExcluded(Constants.ALL) && excludedForShip) ||
                (!excludedForShip && newExclusionss != null && newExclusionss.containsKey(itemId) && newExclusionss.get(itemId)
                        .isPickExcluded(Constants.ALL))) {
            isExcluded = true;
        }
        log.debug("Time taken to verify if item has exclusions across stores is {} (ms)",
                (System.nanoTime() - startTime) / Math.pow(10, 6));
        return isExcluded;
    }

    /**
     * Method to return clearance capacity percentage
     * @param capacityDetails
     * @param inventoryKey
     * @param location
     * @param asyncResults
     * @param propertiesConfig
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Integer getClearanceCapacityPercentage(Map<String, NodeCapacity> capacityDetails, String inventoryKey, Location location,
                                                  YihAsyncResultsHandler asyncResults, PropertiesConfig propertiesConfig) throws ExecutionException, InterruptedException {
        String[] split = inventoryKey.split("\\|");
        String orgCode = split[0];
        String storeId = split[1];
        String itemID = split[2];
        BitSet markDownStatus = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID)))
                .orElse(ItemInventory.builder().markDownStatus(new BitSet()).build()).getMarkDownStatus();
        boolean isClearance = markDownStatus != null &&
                markDownStatus.get(asyncResults.getLocationIndexFuture().get().get(storeId)) &&
                Boolean.parseBoolean(event.getPropertyValue(Constants.FEATURE_COMPUTE_CLEARANCE_CAPACITY_ENABLED));
        NodeCapacity nodeCapacity = capacityDetails
                .get(orgCode + Constants.SEPARATOR + storeId
                        + Constants.SEPARATOR + Util.reportDate(location));


        if (nodeCapacity.getClearanceCapacityPercentage() == null) {
            nodeCapacity.setClearanceCapacityPercentage(Double
                    .parseDouble(propertiesConfig.getPropertyValue(DEFAULT_CLEARANCE_CAPACITY_PERCENTAGE))) ;
        }

        Integer capacity = isClearance ? Optional.ofNullable(nodeCapacity.getClearanceCapacityPercentage()).orElse(0.0).intValue() : 0;
        log.debug("Clearance capacity percentage is set to {} for store {} as it is clearance active {} ",capacity, storeId,isClearance);
        return capacity;
    }

    /**
     * This method updates the multiread response with the exclusion and price fields as part of the new Feature Exclusions
     * @param itemsForExclusionUpdate
     * @param inventoryValueForOutput
     * @param asyncResults
     * @param updatedExclusions
     * @param exclusionFeatureFlags
     */
    public void updateMultiReadResponseWithFeatureExclusions(Map<String, Boolean> itemsForExclusionUpdate, Map<String, Integer> inventoryValueForOutput,
                                                             YihAsyncResultsHandler asyncResults, Map<String, Map<String, Boolean>> updatedExclusions,
                                                             ExclusionFeatureFlags exclusionFeatureFlags, PropertiesConfig propertiesConfig ) {

        itemsForExclusionUpdate.entrySet().stream().filter(exclUpdatMap -> exclUpdatMap.getValue().equals(Boolean.TRUE))
                .forEach(exclUpdMap ->
                        {
                            ItemPrice itemPrice = null;
                            Map<String, ItemPrice> itemPriceMap = new HashMap<>();


                            Set<String> itemidset = new HashSet<>();
                            itemidset.add(exclUpdMap.getKey());
                            itemPriceMap = Util.getAllFromHazelcast(hazelcastInstance, Constants.JCP_ITEM_CURRENT_PRICE, itemidset);
                            itemPrice = itemPriceMap.get(exclUpdMap.getKey());

                            inventoryValueForOutput.put(Constants.SUBLOT_SFS_EXCL_FLAG, updatedExclusions.get(exclUpdMap.getKey()).get(Constants.SUBLOT_SFS_EXCL) ? 1 : 0);
                            inventoryValueForOutput.put(Constants.SUBLOT_BOPUS_EXCL_FLAG, updatedExclusions.get(exclUpdMap.getKey()).get(Constants.SUBLOT_BOPUS_EXCL) ? 1 : 0);
                            inventoryValueForOutput.put(Constants.PERF_SFS_EXCL_FLAG, updatedExclusions.get(exclUpdMap.getKey()).get(PERF_SFS_EXCL) ? 1 : 0);
                            inventoryValueForOutput.put(Constants.PERF_BOPUS_EXCL_FLAG, updatedExclusions.get(exclUpdMap.getKey()).get(PERF_BOPUS_EXCL) ? 1 : 0);

                            inventoryValueForOutput.put(Constants.BELOW_AUR,
                                    itemCurrentPriceUtil.hasAURBasedExclusion(itemPrice, propertiesConfig, true) || itemCurrentPriceUtil.hasAURBasedExclusion(itemPrice, propertiesConfig, false) ? 1 : 0);
                            inventoryValueForOutput.put(Constants.ON_CLEARANCE,
                                    itemCurrentPriceUtil.hasClearanceBasedExclusion(itemPrice, propertiesConfig, true) || itemCurrentPriceUtil.hasClearanceBasedExclusion(itemPrice, propertiesConfig, false) ? 1 : 0);

                            /**The active's and atp's should be set to zero in the multiread response when run in purifyAtp with isUpdateExclusions true to remove the difference
                             * itemInventory table exclusion indicator and that present in exclusion map and/or item current price
                             */
                            if (inventoryValueForOutput.get(Constants.SUBLOT_SFS_EXCL_FLAG) == 1 || inventoryValueForOutput.get(PERF_SFS_EXCL_FLAG) == 1
                                    || inventoryValueForOutput.get(BELOW_AUR) == 1 || inventoryValueForOutput.get(ON_CLEARANCE) == 1) {
                                inventoryValueForOutput.put(SHIP_ACTIVE, 0);
                                inventoryValueForOutput.put(SHIP_ATP, 0);
                            }
                            /**
                             * Bopus does not have AUR check. For Clearance, only onClearance field will be referred to
                             * For Exclusion, only flags will be checked for
                             */
                            if (inventoryValueForOutput.get(Constants.SUBLOT_BOPUS_EXCL_FLAG) == 1 || inventoryValueForOutput.get(Constants.PERF_BOPUS_EXCL_FLAG) == 1
                                    || (inventoryValueForOutput.get(ON_CLEARANCE) == 1 && itemCurrentPriceUtil.isFeatureEnabledFor(exclusionFeatureFlags, true, false))
                                    || (inventoryValueForOutput.get(BELOW_AUR) == 1 && itemCurrentPriceUtil.isFeatureEnabledFor(exclusionFeatureFlags, false, false))
                            ) {
                                inventoryValueForOutput.put(BOPUS_ACTIVE, 0);
                                inventoryValueForOutput.put(BOPUS_ATP, 0);
                            }
                        }
                );
        itemsForExclusionUpdate.entrySet().stream().filter(exclUpdatMap -> exclUpdatMap.getValue().equals(Boolean.FALSE))
                .forEach(exclUpdateMap ->
                        {
                            try {
                                ItemInventory itemInvVal = asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(exclUpdateMap.getKey()));
                                if (itemInvVal != null){
                                    inventoryValueForOutput.put(Constants.SUBLOT_SFS_EXCL_FLAG, itemInvVal.isSubLotSFSExclusion() ? 1 : 0);
                                    inventoryValueForOutput.put(Constants.SUBLOT_BOPUS_EXCL_FLAG, itemInvVal.isSubLotBopusExclusion() ? 1 : 0);
                                    inventoryValueForOutput.put(Constants.PERF_SFS_EXCL_FLAG, itemInvVal.isPerformanceSFSExclusion() ? 1 : 0);
                                    inventoryValueForOutput.put(Constants.PERF_BOPUS_EXCL_FLAG, itemInvVal.isPerformanceBopusExclusion() ? 1 : 0);
                                    inventoryValueForOutput.put(BELOW_AUR, itemInvVal.isBelowaur() ? 1 : 0);
                                    inventoryValueForOutput.put(ON_CLEARANCE, itemInvVal.isOnclearance() ? 1 : 0);
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("Error in getting Item Inventory for itemid {} is {}", exclUpdateMap.getKey(), e);
                            }
                        }

                );
    }

    /**
     *
     * @param itemsForUpdate
     * @param inventoryKey
     * @param inventoryValueForOutput
     * @param locnInvAtpForUpd
     * @param enterpriseAtpForUpd

     */

    public void prepareRecordsForUpdate(Map<String, Boolean> itemsForUpdate, String inventoryKey, Map<String, Integer> inventoryValueForOutput,
                                        Map<String, Map<String, Integer>> locnInvAtpForUpd,
                                        Map<String, Map<String, Integer>> enterpriseAtpForUpd) {
        log.debug("prepareRecordsForUpdate with bopusAtp : {}, shipAtp : {} and  whseAtp : {}", inventoryValueForOutput.get("grossBopusAtp"),
                inventoryValueForOutput.get("grossShipAtp"), inventoryValueForOutput.get("grossWhseAtp"));
        int grossWhseAtp = inventoryValueForOutput.get("grossWhseAtp");
        int grossShipAtp = inventoryValueForOutput.get("grossShipAtp");
        int grossBopusAtp = inventoryValueForOutput.get("grossBopusAtp");
        if (itemsForUpdate.get(inventoryKey.split("\\|")[2]) != null
                && itemsForUpdate.get(inventoryKey.split("\\|")[2])) {
            Map<String, Integer> invAllForUpd = new HashMap<String, Integer>();
            invAllForUpd.putAll(inventoryValueForOutput);
            //Override the ATP with the gross ATP as we will set the correct ship and Bopus ATP even when there are exclusions and shutdown
            invAllForUpd.put(Constants.SHIP_ATP, grossShipAtp);
            invAllForUpd.put(Constants.BOPUS_ATP, grossBopusAtp);
            locnInvAtpForUpd.put(inventoryKey, invAllForUpd);

            String enterpriseInvKey =
                    inventoryKey.split("\\|")[0] + Constants.SEPARATOR + inventoryKey.split("\\|")[2]
                            + Constants.SEPARATOR + inventoryKey.split("\\|")[3];
            Map<String, Integer> entInvAll = enterpriseAtpForUpd.get(enterpriseInvKey);
            int whseAtp = 0;
            int storeShipAtp = 0;
            int storeBopusAtp = 0;
            int onhandSupply = 0;
            int reservedDemand = 0;
            int unconfirmedDemand = 0;
            int totalAva = 0;

            if (entInvAll != null) {
                whseAtp = entInvAll.get("whseatp");
                storeShipAtp = entInvAll.get(Constants.SHIP_ATP);
                storeBopusAtp = entInvAll.get(Constants.BOPUS_ATP);
                onhandSupply = entInvAll.get(Constants.ON_HAND_SUPPLY);
                reservedDemand = entInvAll.get(Constants.RESERVED_DEMAND);
                unconfirmedDemand = entInvAll.get(Constants.UNCONFIRMED_DEMAND);
                totalAva = entInvAll.get(Constants.TOTAL_AVA);
                log.debug(
                        "Found an entry for {} hence aggregating the ATP with old ship ATP: {} and old bopus ATP: {}",
                        enterpriseInvKey, storeShipAtp, storeBopusAtp);
            } else {
                entInvAll = new HashMap<String, Integer>();
            }
            whseAtp += Util.getPostiveInt(grossWhseAtp);
            storeBopusAtp += Util.getPostiveInt(grossBopusAtp);
            storeShipAtp += Util.getPostiveInt(grossShipAtp);
            onhandSupply += Util.getPostiveInt(invAllForUpd.get(Constants.ON_HAND_SUPPLY));
            reservedDemand += Util.getPostiveInt(invAllForUpd.get(Constants.RESERVED_DEMAND));
            unconfirmedDemand += Util.getPostiveInt(invAllForUpd.get(Constants.UNCONFIRMED_DEMAND));
            totalAva += Util.getPostiveInt(invAllForUpd.get(Constants.TOTAL_AVA));

            entInvAll.put("whseatp", whseAtp);
            entInvAll.put(Constants.SHIP_ATP, storeShipAtp);
            entInvAll.put(Constants.BOPUS_ATP, storeBopusAtp);
            entInvAll.put(Constants.ON_HAND_SUPPLY, onhandSupply);
            entInvAll.put(Constants.RESERVED_DEMAND, reservedDemand);
            entInvAll.put(Constants.UNCONFIRMED_DEMAND, unconfirmedDemand);
            entInvAll.put(Constants.TOTAL_AVA, totalAva);
            entInvAll.put(SHIP_ACTIVE, inventoryValueForOutput.get(SHIP_ACTIVE));
            entInvAll.put(BOPUS_ACTIVE, inventoryValueForOutput.get(BOPUS_ACTIVE));

            entInvAll.put("isSubLotSFSExclusion", inventoryValueForOutput.get("isSubLotSFSExclusion"));
            entInvAll.put("isSubLotBopusExclusion", inventoryValueForOutput.get("isSubLotBopusExclusion"));
            entInvAll.put("isPerformanceSFSExclusion", inventoryValueForOutput.get("isPerformanceSFSExclusion"));
            entInvAll.put("isPerformanceBopusExclusion", inventoryValueForOutput.get("isPerformanceBopusExclusion"));
            entInvAll.put("isBelowAur", inventoryValueForOutput.get("isBelowAur"));
            entInvAll.put("isOnClearance", inventoryValueForOutput.get("isOnClearance"));


            enterpriseAtpForUpd.put(enterpriseInvKey, entInvAll);

        }
    }



    public void doWriteBehindForUpdateModeItems(Map<String, Map<String, Integer>> locnInvAtpForUpd,
                                                Map<String, Map<String, Integer>> enterpriseAtpForUpd, Map<String, Boolean> itemsForUpdate, Map<String, Boolean> itemsForExclusionsUpdate,
                                                Map<String, Boolean> itemsForpublish, Map<String, ExclusionLocations> newExclusionss, Map<ItemPriceKey, ItemPrice> currPrice,
                                                Map<String, ItemDetails> itemDetails, PropertiesConfig propertiesConfig, ShipActiveCalculatorUtil shipActiveCalculatorUtil, Producer producer) {

        if (Boolean.valueOf(event.getProperties().get("yih.location.inv.update.onwritebehind"))) {
            locnInvAtpForUpd.entrySet().stream()
                    .forEach(entry -> Util
                            .put(hazelcastInstance, JCP_LOCATION_INVENTORY_POJO,
                                    LocationInventoryKey.fromString(entry.getKey()),
                                    LocationInventoryInfo.fromMap(entry.getValue(), null)));
        }

        IMap<ItemInventoryKey, ItemInventory> enInvMap = hazelcastInstance.getMap(JCP_ITEM_INVENTORY);

        if (currPrice == null || currPrice.isEmpty())
            currPrice = hazelcastInstance.getMap(JCP_ITEM_CURRENT_PRICE);

        final Map<ItemPriceKey, ItemPrice> finalCurrPrice = currPrice;
        enterpriseAtpForUpd.entrySet().stream()
                .forEach(entry -> {
                    ItemInventory itemInventory = enInvMap
                            .get(new ItemInventoryKey(entry.getKey().split("\\|")[1]));
                    ItemInventory oldItemInventory = itemInventory;
                    if (itemInventory == null) {
                        itemInventory = ItemInventory.builder().itemID(entry.getKey().split("\\|")[1]).build();
                    }
                    int shipActive = 1;
                    int bopusActive = 1;
                    Map<String, Integer> entInvAll = entry.getValue();
                    boolean hasPriceBasedExclusionForShip = false;
                    boolean hasPriceBasedExclusionForBopus = false;

                    ItemPriceKey itemPriceKey =   new ItemPriceKey(entry.getKey().split("\\|")[1]);
                    hasPriceBasedExclusionForShip =  itemCurrentPriceUtil.hasPriceBasedExclusion(finalCurrPrice.get(itemPriceKey), propertiesConfig, true);
                    hasPriceBasedExclusionForBopus =  itemCurrentPriceUtil.hasPriceBasedExclusion(finalCurrPrice.get(itemPriceKey), propertiesConfig, false);


                    if (!shipActiveCalculatorUtil.isShipActive(entry.getKey().split("\\|")[1],getExclusionProperties(event))
                            || hasPriceBasedExclusionForShip) {
                        shipActive = 0;
                    }

                    if (isExcludedAcrossStoresForPick(entry.getKey().split("\\|")[1], newExclusionss)
                            || hasPriceBasedExclusionForBopus) {
                        bopusActive = 0;
                    }
                    if(entInvAll.get("whseatp") != null && entInvAll.get("whseatp") > 0){
                        itemInventory.setWhseatp(entInvAll.get("whseatp"));
                    }
                    itemInventory.setBopusActive(bopusActive);
                    itemInventory.setShipActive(shipActive);
                    itemInventory.setShipATP(entInvAll.get(SHIP_ATP));
                    itemInventory.setBopusATP(entInvAll.get(BOPUS_ATP));
                    itemInventory.setOnhandsupply(entInvAll.get(ON_HAND_SUPPLY));
                    itemInventory.setUnconfirmedDemand(entInvAll.get(UNCONFIRMED_DEMAND));
                    itemInventory.setReservedDemand(entInvAll.get(RESERVED_DEMAND));
                    itemInventory.setPurifyATPDone(true);
                    itemInventory.setNetworkQualStatus(getNetworkQualitativeStatus(itemInventory, itemInventory.getActiveStoresATP(), propertiesConfig));
                    ItemInventoryUtil.generateQualitativeStatus(hazelcastInstance,oldItemInventory, itemInventory,false, propertiesConfig);
                    ItemInventory inventory = enInvMap
                            .put(new ItemInventoryKey(entry.getKey().split("\\|")[1]), itemInventory);
                    boolean computeQsEnabled = Boolean.parseBoolean(propertiesConfig.getPropertyValue(COMPUTE_QS_ENABLED));
                    if (itemsForpublish != null && itemsForpublish.get(entry.getKey().split("\\|")[1]) != null
                            && itemsForpublish.get(entry.getKey().split("\\|")[1])) {
                        // to publish NA when sublot exclsuion is applied through purifyATP.
                        if(computeQsEnabled && ItemInventoryUtil.isPresentInCapacityExclusionMap(itemInventory, hazelcastInstance)){
                            ItemInventory tempItemInventory = new ItemInventory(itemInventory);
                            tempItemInventory.setNetworkQualStatus(0);
                            producer.publish(YihInventoryUtil.createEventData(tempItemInventory).getMessage());
                        } else {
                            producer.publish(YihInventoryUtil.createEventData(itemInventory).getMessage());
                        }
                    }
                    log.debug("Updating purified atp for itemInventoryKey {} value {} resp {} ",
                            new ItemInventoryKey(entry.getKey().split("\\|")[1]), itemInventory, inventory);
                });
        Set<String> itemIDs = itemsForUpdate.keySet();
        for (String itemID : itemIDs) {
            if ((itemsForUpdate.get(itemID) != null && itemsForUpdate.get(itemID)) && !enterpriseAtpForUpd
                    .containsKey("JCP|" + itemID + "|EACH")) {
                ItemInventory itemInventory = enInvMap.get(new ItemInventoryKey(itemID));

                if (itemInventory == null) {
                    itemInventory = ItemInventory.builder().itemID(itemID).build();
                }
                itemInventory.setPurifyATPDone(true);
                ItemInventory inventory = enInvMap.put(new ItemInventoryKey(itemID), itemInventory);
                if (itemsForpublish != null && itemsForpublish.get(itemID) != null && itemsForpublish
                        .get(itemID)) {
                    if (inventory != null) {
                        producer.publish(YihInventoryUtil.createEventData(inventory).getMessage());
                    } else {
                        log.debug("Inventory for the itemId: {} is null. Not publishing the status" , itemID);
                    }

                }
                log.debug("Updating purified atp for itemInventoryKey {} value {} resp {} ",
                        new ItemInventoryKey(itemID), itemInventory, inventory);
            }
        }
    }


    /**
     * When set to true, it will look into the master data and then set the exclusion flags in the item inventory table.
     * This has been introduced to update the data when a mismatch is detected
     * @param itemID
     * @param isUpdateExcl
     * @param enInvMap
     * @param producer
     * @return
     */
    public Map<String, Map<String, Boolean>> calculateFeatureLevelExclusions(String itemID, Boolean isUpdateExcl, IMap<ItemInventoryKey, ItemInventory> enInvMap,
                                                                             Producer producer) {

        Map<String, Map<String, Boolean>> sublotexclusionAllocationMap = null;

        if (isUpdateExcl) {

            boolean perfSFSExclusion = false;
            boolean perfBOPUSExclusion = false;
            boolean subLotSFSExclusion = false;
            boolean subLotBOPUSExclusion = false;

            ItemInventory itemInventory = enInvMap.get(new ItemInventoryKey(itemID));

            IMap<String, ExclusionContainer> exclusionMap = hazelcastInstance.getMap(JCP_EXCLUSION_MAP);

            Map<String, Boolean> exclusionCalcMap = new HashMap<>();
            checkForExclusions(itemID, exclusionMap, exclusionCalcMap);
            boolean productLineExcl = exclusionCalcMap.get("ProductLineExcl");
            boolean perfExcl = exclusionCalcMap.get("PerformanceExclusion");
            boolean sublotExcl = exclusionCalcMap.get("SublotExclusion");

            if (productLineExcl) {
                ExclusionContainer exclusionContainer = exclusionMap.get(itemID.substring(0, 3));
                if (perfExcl) {
                    ExclusionLocations exclLocForPerfExcl = exclusionContainer.getExclusions().get(itemID);
                    if (exclLocForPerfExcl.getShipLocations() != null && exclLocForPerfExcl.getShipLocations().contains("ALL"))
                        perfSFSExclusion = true;

                    if (exclLocForPerfExcl.getPickLocations() != null && exclLocForPerfExcl.getPickLocations().contains("ALL"))
                        perfBOPUSExclusion = true; }

                if (sublotExcl) {

                    ExclusionLocations exclLocForSubLotExcl = exclusionContainer.getExclusions().get(itemID.substring(0, 7));

                    if (exclLocForSubLotExcl.getShipLocations() != null && exclLocForSubLotExcl.getShipLocations().contains("ALL"))
                        subLotSFSExclusion = true;

                    if (exclLocForSubLotExcl.getPickLocations() != null && exclLocForSubLotExcl.getPickLocations().contains("ALL"))
                        subLotBOPUSExclusion = true;
                }
            }
            Map<String, Boolean> subLotExclForItemID = ImmutableMap.of(Constants.SUBLOT_SFS_EXCL, subLotSFSExclusion , Constants.SUBLOT_BOPUS_EXCL, subLotBOPUSExclusion,
                    Constants.PERF_SFS_EXCL, perfSFSExclusion , Constants.PERF_BOPUS_EXCL, perfBOPUSExclusion);

            sublotexclusionAllocationMap = ImmutableMap.of(itemID, subLotExclForItemID);
            ItemInventory inventory = enInvMap.put(new ItemInventoryKey(itemID), itemInventory);

            producer.publish(YihInventoryUtil.createEventData(inventory).getMessage());


        } else {
            log.debug("isUpdateExclusions not set to true for itemid {}", itemID);
        }

        return sublotexclusionAllocationMap;
    }

    /**
     * This method forms itemInventory Keys to query jcp_location table
     * @param inventoryQrys
     * @param distributionGroupStores
     * @param locationDetails
     * @param itemKeys
     * @param isPurifyAtpRequest
     @return
     */
    public Set<String> formInventoryKeys(List<InventoryQuerry> inventoryQrys, Set<String> distributionGroupStores,
                                         Map<String, Location> locationDetails, Set<String> itemKeys, boolean isPurifyAtpRequest) {
        Set<String> invKeys = new HashSet<String>();


        inventoryQrys.stream().forEach(inventoryQuerry -> {
                    if (inventoryQuerry.getStoreID() == null ||
                            (Objects.nonNull(inventoryQuerry.getDistributionGroup()) && !inventoryQuerry.getDistributionGroup().equals(Constants.JCP_WHSE_DG))) {
                        if ((inventoryQuerry.getIsUpdateMode() != null && inventoryQuerry.getIsUpdateMode())
                                || (inventoryQuerry.isUpdateStoreAtps())
                                || (inventoryQuerry.isUpdateWhseAtp())) {
                            itemKeys.add(inventoryQuerry.getItemID());

                            inventoryQuerry.setDistributionGroup(ALL);
                            inventoryQuerry.setNodeType(ALL);
                        }
                        if (!isPurifyAtpRequest
                                || (isPurifyAtpRequest && (inventoryQuerry.getIsUpdateMode() || inventoryQuerry.isUpdateStoreAtps() || inventoryQuerry.isUpdateWhseAtp()))) {
                            distributionGroupStores.stream().forEach(storeID -> {
                                        Location location = locationDetails.get(storeID);
                                        if (Objects.nonNull(location) && StringUtils.isNoneBlank(location.getNodeType())) {
                                            String nodeType = location.getNodeType();
                                            boolean nodeForDG = true;
                                            if (!inventoryQuerry.getDistributionGroup().equals(ALL)) {
                                                boolean storeFlag = inventoryQuerry.getDistributionGroup().equals(JCP_SFS_DG) &&
                                                        (nodeType.equals(NodeType.Store.name()) || nodeType.equals(ALL));
                                                boolean whseFlag = inventoryQuerry.getDistributionGroup().equals(JCP_WHSE_DG) &&
                                                        !nodeType.equals(NodeType.Store.name());
                                                nodeForDG = storeFlag || whseFlag;
                                            }
                                            if (nodeForDG && (inventoryQuerry.getNodeType().equals(ALL) ||
                                                    inventoryQuerry.getNodeType().equals(nodeType))) {
                                                if (isPurifyAtpRequest) {
                                                    if (((inventoryQuerry.isUpdateStoreAtps() && NodeType.Store.name()
                                                            .equals(location.getNodeType()))
                                                            || (inventoryQuerry.isUpdateWhseAtp() && !NodeType.Store.name()
                                                            .equals(location.getNodeType())))) {
                                                        invKeys.add(
                                                                "JCP|" + storeID.split("\\|")[1] + Constants.SEPARATOR
                                                                        + inventoryQuerry
                                                                        .getItemID()
                                                                        + Constants.SEPARATOR + inventoryQuerry.getUom());
                                                    }
                                                } else {
                                                    invKeys.add(
                                                            "JCP|" + storeID.split("\\|")[1] + Constants.SEPARATOR
                                                                    + inventoryQuerry
                                                                    .getItemID()
                                                                    + Constants.SEPARATOR + inventoryQuerry.getUom());
                                                }

                                            }
                                        } else {
                                            log.info("Location or NodeType details is not available for the node Id{}",
                                                    storeID);
                                        }
                                    }
                            );
                        }
                    } else {
                        if(Objects.nonNull(inventoryQuerry.getStoreID()))
                            invKeys.add("JCP|" + inventoryQuerry.getStoreID() + Constants.SEPARATOR + inventoryQuerry
                                    .getItemID() + Constants.SEPARATOR + inventoryQuerry.getUom());
                    }
                }
        );

        return invKeys;
    }


    /**
     * This method extracts out the location inventory of the itemIDs in the request for multiread
     * @param invkeys
     * @return
     */
    public Map<String, Map<String, Integer>> getInventory(Set<String> invkeys) {
        long l = System.currentTimeMillis();
        IMap<LocationInventoryKey, LocationInventoryInfo> inventoryMap = hazelcastInstance
                .getMap(Constants.JCP_LOCATION_INVENTORY_POJO);
        Set<LocationInventoryKey> actualInvKeys = invkeys.stream()
                .filter(Objects::nonNull)
                .map(LocationInventoryKey::fromString)
                .collect(toSet());
        Map<LocationInventoryKey, LocationInventoryInfo> inventoryData =
                Boolean.valueOf(event.getProperties().get("yih.maps.readFromCassandra")) ? inventoryMap
                        .getAll(actualInvKeys) : Util.getAllFromHazelcast(hazelcastInstance,
                        Constants.JCP_LOCATION_INVENTORY_POJO, actualInvKeys);
        Map<String, Map<String, Integer>> inventory = inventoryData
                .entrySet()
                .stream()
                .filter(entry -> entry != null && entry.getKey() != null)
                .collect(
                        HashMap::new,
                        (mapp, entry) -> mapp.put(
                                entry.getKey().toStringKey(),
                                Optional.ofNullable(entry.getValue()).map(LocationInventoryInfo::toMap).orElse(null)
                        ),
                        Map::putAll
                );
        if(Boolean.valueOf(event.getProperties().get("yih.multiread.maploadlogging.enabled")))
            log.info("Time Taken to get jcp_location_inventory from HZ-Thread: {} during multi-read is: {}",
                    Thread.currentThread().getId(), (System.currentTimeMillis() - l));
        return inventory;
    }


    /**
     * This method returns EntryView of JCP_LOCATION table
     * locations will have its key as JCP|storeid and value of Location
     * @param hazelcastInstance
     * @param distributionGroupStores
     * @return
     */
    public Map<String, Location> getLocations(HazelcastInstance hazelcastInstance,
                                              Set<String> distributionGroupStores) {
        long l = System.currentTimeMillis();
        IMap<String, Location> locMap = hazelcastInstance.getMap(Constants.JCP_LOCATION);
        Map<String, Location> locationDetails = new HashMap<>();
        if (Boolean.valueOf(event.getProperties().get("yih.maps.readFromCassandra"))) {
            locationDetails = locMap.getAll(distributionGroupStores);
        } else {
            locationDetails = Util
                    .getAllFromHazelcast(hazelcastInstance, Constants.JCP_LOCATION, distributionGroupStores);
        }
        if(Boolean.valueOf(event.getProperties().get("yih.multiread.maploadlogging.enabled")))
            log.info("Time Taken to get jcp_location from HZ-Thread: {} during multi-read is: {}",
                    Thread.currentThread().getId(), (System.currentTimeMillis() - l));
        return locationDetails;
    }


    /**
     * This method returns the EntryView of JCP_ITEM table
     * @param hazelcastInstance
     * @param inventoryQrys
     * @param orgCode

     * @return
     */
    public Map<String, ItemDetails> getItemDetails(HazelcastInstance hazelcastInstance,
                                                   List<InventoryQuerry> inventoryQrys, String orgCode, Map<String, Location> locationDetails)
    {
        long l = System.currentTimeMillis();
        IMap<String, ItemDetails> itemMap = hazelcastInstance.getMap(Constants.JCP_ITEM);
        Set<String> items = new HashSet<String>();
        Set<String> preferSubItems = new HashSet<>();
        Set<String> preferredSubsItems = new HashSet<>();

        inventoryQrys.forEach(inventoryQuerry -> {
                    items.add(orgCode + Constants.SEPARATOR + inventoryQuerry.getItemID() + Constants.SEPARATOR
                            + inventoryQuerry.getUom());
                }
        );

        Map<String, ItemDetails> itemDetails = new HashMap<>();
        if (Boolean.valueOf(event.getProperties().get("yih.maps.readFromCassandra"))) {
            itemDetails = itemMap.getAll(items);
        } else {
            itemDetails = Util.getAllFromHazelcast(hazelcastInstance, Constants.JCP_ITEM, items);
        }

        if(Boolean.valueOf(event.getProperties().get("yih.multiread.maploadlogging.enabled")))
            log.info("Time Taken to get jcp_item from HZ-Thread: {} during multi-read is : {}", Thread.currentThread().getId(),
                    (System.currentTimeMillis() - l));
        return itemDetails;
    }

    /**
     * Takes storeDGMap and whseDGMap as input and calculates size for
     * store and whse atp blob
     * @param storeDGMap
     * @param whseDGMap
     */
    public void setAtpBlobSizes(Map<Integer, List<String>> storeDGMap,
                                Map<Integer, List<String>> whseDGMap, int storeAtpBlobSize, int whseAtpBlobSize) {
        storeAtpBlobSize = storeDGMap != null ? storeDGMap.get(1).size() : 0;
        whseAtpBlobSize = whseDGMap != null ? whseDGMap.get(1).size() : 0 ;
    }


    /**
     * This method returns TRUE if belowaur exclusion is present , else false
     * @param propertiesConfig
     * @param exclusionFeatureFlags
     * @param forShip
     * @param itemPrice
     * @param currPrice
     * @return
     */
    public boolean isExclusionFeatureEnabled(PropertiesConfig propertiesConfig, ExclusionFeatureFlags exclusionFeatureFlags, boolean forShip, ItemPrice itemPrice, Map<ItemPriceKey, ItemPrice> currPrice){

        Boolean isPriceExclusionEnabled = Boolean.parseBoolean(propertiesConfig.getPropertyValue(Constants.FEATURE_EXCLUSION_PRICE_ENABLED));
        Boolean isAurExclusionMultireadEnabled = Boolean.parseBoolean(propertiesConfig.getPropertyValue(FEATURES_EXCLUSION_PRICE_AUR_EXCLUSION_ON_MULTIREAD));
        boolean isAurExcluded = false;
        boolean isFeatureExcluded = false;

        if((currPrice != null && !currPrice.isEmpty()) && itemPrice != null && itemPrice.getPriceType() != PriceType.CLEARANCE){
            if(forShip){
                Boolean isSFSExcluded = itemCurrentPriceUtil.isFeatureEnabledFor(exclusionFeatureFlags, false, forShip);
                if(isPriceExclusionEnabled && isAurExclusionMultireadEnabled && isSFSExcluded){
                    isFeatureExcluded = true;
                }
            }else{
                Boolean isBOPUSExcluded = itemCurrentPriceUtil.isFeatureEnabledFor(exclusionFeatureFlags, false, forShip);
                if(isPriceExclusionEnabled && isAurExclusionMultireadEnabled && isBOPUSExcluded){
                    isFeatureExcluded = true;
                }
            }
            if(isFeatureExcluded){
                isAurExcluded = itemCurrentPriceUtil.isAurExclusionApplicable(itemPrice, propertiesConfig);
            }
        }
        return isAurExcluded;
    }

    private boolean isPickEligible(String inventoryKey, ItemDetails itemdetails, String storeId,
                                   Map<String, ExclusionLocations> newExclusionss) {
        boolean isPickEligible = true;
        long startTime = System.nanoTime();
        if (itemdetails != null && itemdetails.getProductLine() != null
                && itemdetails.getLotNumber() != null) {
            if (newExclusionss != null && newExclusionss.containsKey(itemdetails.getItemID())
                    && newExclusionss.get(itemdetails.getItemID()).isPickExcluded(storeId)) {
                isPickEligible = false;
            }
        } else {
            log.warn("Returning true as pick item detials are null {}", itemdetails);
        }
        log.debug("Time taken to check for the pick eligible in {} (ms)",
                (System.nanoTime() - startTime) / Math.pow(10, 6));

        return isPickEligible;
    }

    private boolean isShipEligible(String inventoryKey,ItemDetails itemdetails, String storeId,
                                   Map<String, ExclusionLocations> newExclusionss) {
        boolean isShipEligible = true;
        long startTime = System.nanoTime();
        if (itemdetails != null && itemdetails.getProductLine() != null
                && itemdetails.getLotNumber() != null) {
            if (newExclusionss != null && newExclusionss.containsKey(itemdetails.getItemID())
                    && newExclusionss.get(itemdetails.getItemID()).isShipExcluded(storeId)) {
                isShipEligible = false;
            }
        } else {
            log.warn("Returning true as ship item details are null {}", itemdetails);
        }

        log.debug("Time taken to check for the ship eligible in isShipELigible {} (ms)",
                (System.nanoTime() - startTime) / Math.pow(10, 6));
        return isShipEligible;

    }

    private boolean getExclusionsIfPickEligible(String inventoryKey, ItemDetails itemdetails, String storeID,
                                                Map<String, ExclusionLocations> newExclusionss, boolean isSFSExcluded, boolean isAurExcluded) {
        if(!getBoolean(Constants.YIH_FEATURE_LEVEL_EXCLUSION_CONTROL_ENABLED))
            return !isPickEligible(inventoryKey, itemdetails, storeID,
                    newExclusionss) || isSFSExcluded || isAurExcluded;
        else
            return false;
    }

    //New methods introduced as part of INV-8560
    private boolean getExclusionsIfShipEligible( String inventoryKey, ItemDetails itemdetails, String storeID,
                                                 Map<String, ExclusionLocations> newExclusionss, boolean isSFSExcluded, boolean isAurExcluded)
    {
        if(!getBoolean(Constants.YIH_FEATURE_LEVEL_EXCLUSION_CONTROL_ENABLED))
            return  !isShipEligible(inventoryKey, itemdetails, storeID,
                    newExclusionss) || isSFSExcluded || isAurExcluded;
        else
            return false;

    }

    //INV-8560 -- New condition added to execute the existing exclusion logic or execute the new exclusions logic
    //newMethod getExclusionsIfPickEligible is introduced to compute the existing logic with Price and Bopus Exclusion conditions
    public void calcShipBopusAtpActiveAfterExclusions (ItemInventory itemInventory, boolean featureLevelExclusionControl, boolean featureExclusionPriceEnabled,
                                                       boolean featureExclusionPriceBopusExcluded, boolean featureExclusionPriceSFSExcluded, boolean isBopusOrSFSExcluded,
                                                       Map<String, Boolean> shutdowns, Location location, String[] inventoryKeySplit, boolean isDirty, String inventoryKey, ItemDetails itemdetails,
                                                       Map<String, ExclusionLocations> newExclusionss, boolean isAurExcluded, Map<String, Integer> inventoryValueForOutput, boolean isShip) {
        int supply = inventoryValueForOutput.get("supply");
        int demand = inventoryValueForOutput.get("demand");

        if (isShip) {
            long timeForShipExclusions = System.currentTimeMillis();
            boolean featureLevelExclusionSFS = false;
            int shipATP = inventoryValueForOutput.get(Constants.SHIP_ATP);
            int grossShipAtp = inventoryValueForOutput.get("grossShipAtp");
            if (getBoolean(Constants.YIH_FEATURE_LEVEL_EXCLUSION_CONTROL_ENABLED))
                featureLevelExclusionSFS = featureLevelExclusionControlUtils.getExclusions(itemInventory, true, isBopusOrSFSExcluded, featureLevelExclusionControl,
                        featureExclusionPriceEnabled, featureExclusionPriceBopusExcluded, featureExclusionPriceSFSExcluded);
            if ((shutdowns.get(inventoryKeySplit[1] + SEPARATOR + ExclusionIndicator.S  + SEPARATOR + Util.reportDate(location)) != null)
                    || isDirty
                    || getExclusionsIfShipEligible(inventoryKey, itemdetails, inventoryKeySplit[1],
                    newExclusionss, isBopusOrSFSExcluded, isAurExcluded)
                    || featureLevelExclusionSFS) {
                if (!isExcludedAcrossStoresForShip(inventoryKeySplit[2], newExclusionss)
                        && !isBopusOrSFSExcluded && !isAurExcluded) {
                    grossShipAtp = 0;
                }
                inventoryValueForOutput.put(SHIP_ACTIVE, 0);
                shipATP = 0;
            } else {
                inventoryValueForOutput.put(SHIP_ACTIVE, 1);
                shipATP = supply - demand - inventoryValueForOutput.get("shipSafetyFactor");
            }
            inventoryValueForOutput.put(Constants.SHIP_ATP, shipATP);
            inventoryValueForOutput.put("grossShipAtp", grossShipAtp);
            log.debug("Time taken to calculate shipExclusions in multi-read is: {}",
                    System.currentTimeMillis() - timeForShipExclusions);
            log.debug("After exclusion logic, shipAtp is {} and grossShipAtp is {}", shipATP,
                    grossShipAtp);

        }
        else if (!isShip) {
            long timeForPickExclusions = System.currentTimeMillis();
            boolean featureLevelExclusionBopus = false;
            int bopusATP = inventoryValueForOutput.get(Constants.BOPUS_ATP);
            int grossBopusAtp = inventoryValueForOutput.get("grossBopusAtp");

            if (getBoolean(YIH_FEATURE_LEVEL_EXCLUSION_CONTROL_ENABLED))
                featureLevelExclusionBopus = featureLevelExclusionControlUtils.getExclusions(itemInventory, false, isBopusOrSFSExcluded, featureLevelExclusionControl,
                        featureExclusionPriceEnabled, featureExclusionPriceBopusExcluded, featureExclusionPriceSFSExcluded);

            if ((shutdowns.get(inventoryKeySplit[1] + Constants.SEPARATOR + ExclusionIndicator.P + Constants.SEPARATOR + Util.reportDate(location)) != null)
                    || isDirty
                    || getExclusionsIfPickEligible(inventoryKey, itemdetails, inventoryKeySplit[1],
                    newExclusionss, isBopusOrSFSExcluded, isAurExcluded)
                    || featureLevelExclusionBopus) {

                bopusATP = 0;
                inventoryValueForOutput.put(Constants.BOPUS_ACTIVE, 0);
                if (!isExcludedAcrossStoresForPick(inventoryKeySplit[2], newExclusionss)
                        && !isBopusOrSFSExcluded && !isAurExcluded) {
                    grossBopusAtp = 0;
                }
            } else {
                inventoryValueForOutput.put(BOPUS_ACTIVE, 1);
                bopusATP = supply - demand - inventoryValueForOutput.get("bopusSafetyFactor");
            }
            inventoryValueForOutput.put(Constants.BOPUS_ATP, bopusATP);
            inventoryValueForOutput.put("grossBopusAtp", grossBopusAtp);
            log.debug("Time taken to calculate pickExclusions in multi-read is: {}",
                    System.currentTimeMillis() - timeForPickExclusions);
            log.debug("After exclusion logic, bopusAtp is {} and grossBopusAtp is {}", bopusATP,
                    grossBopusAtp);
        }
    }

    /*
    As part of multiread api, Method to return available capacity as clearance capacity/standard capacity based on the markdown status of the store for that item
    Based on the flag FEATURE_COMPUTE_CLEARANCE_CAPACITY_ENABLED, clearance capacity will be returned, else regular capacity
 */
    public Integer getAvailableCapacity(Map<String, NodeCapacity> capacityDetails, String inventoryKey, Location location,
                                        YihAsyncResultsHandler asyncResults, PropertiesConfig propertiesConfig) throws ExecutionException, InterruptedException {
        String[] split = inventoryKey.split("\\|");
        String orgCode = split[0];
        String storeId = split[1];
        String itemID = split[2];
        BitSet markDownStatus = Optional.ofNullable(asyncResults.getItemInventoryFuture().get().get(new ItemInventoryKey(itemID)))
                .orElse(ItemInventory.builder().markDownStatus(new BitSet()).build()).getMarkDownStatus();
        //Set available capacity as clearance capacity/standard capacity based on the markdown status of the store for that item
        boolean isClearance = markDownStatus != null &&
                markDownStatus.get(asyncResults.getLocationIndexFuture().get().get(storeId)) &&
                Boolean.parseBoolean(event.getPropertyValue(Constants.FEATURE_COMPUTE_CLEARANCE_CAPACITY_ENABLED));
        NodeCapacity nodeCapacity = capacityDetails
                .get(orgCode + Constants.SEPARATOR + storeId
                        + Constants.SEPARATOR + Util.reportDate(location));
        Integer capacity = isClearance
                ? ClearanceCapacityCalculator.getAvailableClearaceCapacity(nodeCapacity, propertiesConfig)
                : nodeCapacity.getAvailableCapacity();
        log.debug("Available capacity is set to {} for store {} as it is clearance active {} ", capacity, storeId,
                isClearance);
        return capacity;
    }

    /**
     As part of multiread api, Method to return standard capacity
     */
    public Integer getStandardCapacity(Map<String, NodeCapacity> capacityDetails, String inventoryKey, Location location) {
        String[] split = inventoryKey.split("\\|");
        String orgCode = split[0];
        String storeId = split[1];
        return capacityDetails
                .get(orgCode + Constants.SEPARATOR + storeId
                        + Constants.SEPARATOR + Util.reportDate(location))
                .getStandardCapacity();
    }

    /**
     * This method is for bypassing the async calls, exclusion calculations if the the multiread input has storeid of type DC or distributionGroup of WHSE-DG
     * @param hazelcastInstance
     * @param multiReadInputForWHNodeOrWHDG
     * @param locations
     * @param distributionGroupStores
     * @param distributionGroupMap
     * @param isPurifyAtpRequest
     * @param itemDetails
     * @return
     */
    public MultiReadResponseList dlcNodeDistGroupMROutput(HazelcastInstance hazelcastInstance, List<InventoryQuerry> multiReadInputForWHNodeOrWHDG, Map<String,
            Location> locations, Set<String> distributionGroupStores, IMap<String, Map<Integer, List<String>>> distributionGroupMap, boolean isPurifyAtpRequest,
                                                          Map<String, ItemDetails> itemDetails) {

        MultiReadResponseList response = new MultiReadResponseList();
        List<MultiReadResponse> multiReadResponseList = new ArrayList<>();

        Set<String> items = new HashSet<String>();
        multiReadInputForWHNodeOrWHDG.stream().forEach(invquery -> items.add("JCP|" +invquery.getItemID() + "|EACH"));

        //itemDetails has the key as "JCP|itemid|EACH"
        itemDetails = itemDetails.entrySet().stream().filter(entry -> items.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        //Kit changes
        if (Boolean.parseBoolean(propertiesConfig.getPropertyValue(KITCOMPONENT_AVAILABILITY_ENABLED))) {
            log.info("kitComponentAvailEnabled Availability Flag enabled");
            Map<String, Map<String, Integer>> mapOfkitItemIdandComponenet = new HashMap<String, Map<String, Integer>>();

          /*  for (Map.Entry<String, ItemDetails> s : itemDetails.entrySet()) {
                bundleInfo1 = ItemInventoryUtil.getKitComponentDetails(s.getValue());
            }*/
            //for common
            // List<BundleInfo> bundleInfow = ItemInventoryUtil.getKitComponentDetails(itemDetails.get(items));
            List<BundleInfo> bundleInfo = new ArrayList<>();
            for (Map.Entry<String, ItemDetails> entry : itemDetails.entrySet()) {
                if (entry.getValue().isBundle()) {
                    bundleInfo.add(ItemInventoryUtil.getKitComponentDetails(entry.getValue()));
                }
            }
            bundleInfo.stream().forEach(x -> {
                mapOfkitItemIdandComponenet.put(x.getKitItemId(), x.getComponents());
            });
            Set<String> setofKitItemId = mapOfkitItemIdandComponenet.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
            List<String> listOfComponentKey = mapOfkitItemIdandComponenet.entrySet().stream().flatMap(x -> x.getValue().entrySet().stream().map(c -> c.getKey())).collect(Collectors.toList());
            Set<String> setOfComponentKey = mapOfkitItemIdandComponenet.entrySet().stream().flatMap(x -> x.getValue().entrySet().stream().map(c -> c.getKey())).collect(Collectors.toSet());

            //TODO:  "deal with the keys for one store id and DISTRIBUTION GROUP- (WHSE-DG) "
            Set<String> keysforComponent = new HashSet<>();
            for (InventoryQuerry invQuery : multiReadInputForWHNodeOrWHDG)
                if (Objects.nonNull(invQuery.getDistributionGroup()) && invQuery.getDistributionGroup().equals("WHSE-DG")) {
                    listOfComponentKey.stream().forEach(componentItemId-> {
                        List<String> whseStores = getStoresFromDg(JCP_WHSE_DG, new HashSet<>(), null,
                                distributionGroupMap.get(JCP_ORG_CODE + SEPARATOR + JCP_WHSE_DG));
                        whseStores.stream().forEach(whseStore -> keysforComponent.add(whseStore + SEPARATOR + componentItemId + "|EACH"));
                    });
                } else if (invQuery.getStoreID() != null) {
                    listOfComponentKey.stream().forEach(componentItemId -> {
                        keysforComponent.add("JCP|" + invQuery.getStoreID() + "|" + componentItemId + "|EACH");   //pipe and JCP can be used via CONSTANT
                    });
                }

    /*        // we have all the formed keys here for store nad whse-DG
            multiReadInputForWHNodeOrWHDG.stream().forEach(x -> {
                        if (x.getStoreID() != null) {
                            listOfComponentKey.stream().forEach(componentitemID -> {
                                keysforComponent.add("JCP|" + x.getStoreID() + "|" + componentitemID + "|EACH");
                            });
                        }
                    }
            );*/

            //TODO : itemId| storeId [{itemId : 1,DG : whse},{itemId : 2, storeId : 9130}]
            //TODO : A = B:C = 2:1 -> = 20
            //     B= 40 -shipAtp
            //      C= 20; -shipATP   =====>> B = 40/2 , c= 20/1  //compare  B and  C take the minimum for kit A

            Map<String, Map<String, Integer>> componentInventory = getInventory(keysforComponent);
            Set<String> kitItemKeys = formInventoryKeys(multiReadInputForWHNodeOrWHDG, distributionGroupStores, locations, setofKitItemId, isPurifyAtpRequest);
            Iterator<String> kitItemInventoryKeys = kitItemKeys.iterator();
            while (kitItemInventoryKeys.hasNext()) {
                String kitInventoryKey = kitItemInventoryKeys.next();
                String[] kitInventoryKeySplit = kitInventoryKey.split("\\|");
                String kitStoreId = kitInventoryKeySplit[0] + Constants.SEPARATOR + kitInventoryKeySplit[1];  //storeId : JCP|9132
                String kitItemId = kitInventoryKeySplit[2];   //kit item ID 30521900081

                Location location = locations.get(kitStoreId);

                int kitShipATP=0;
                for (String component : mapOfkitItemIdandComponenet.get(kitItemId).keySet()) {
                    int qty =  mapOfkitItemIdandComponenet.get(kitItemId).get(component);
                    String key = kitStoreId + Constants.SEPARATOR + component + Constants.SEPARATOR + Constants.UOM_EACH;
                    Map<String , Integer> componentItemInventory = componentInventory.get(key);
                    if (Objects.nonNull(componentItemInventory)) {

                        int supply = 0;
                        int demand = 0;

                        if (componentItemInventory.containsKey(Constants.ON_HAND_SUPPLY)) {
                            supply = componentItemInventory.get(Constants.ON_HAND_SUPPLY);
                        }
                        if (componentItemInventory.containsKey(Constants.UNCONFIRMED_DEMAND)) {
                            demand = demand + componentItemInventory.get(Constants.UNCONFIRMED_DEMAND);
                        }
                        if (componentItemInventory.containsKey(Constants.RESERVED_DEMAND)) {
                            demand = demand + componentItemInventory.get(Constants.RESERVED_DEMAND);
                        }

                        int shipATPComponenet = supply-demand;   //40
                        int noOfkitform = shipATPComponenet/qty;

                        if(noOfkitform==0)
                            break;
                        if(kitShipATP==0)
                            kitShipATP=noOfkitform;

                        if(noOfkitform<kitShipATP)
                            kitShipATP=noOfkitform;
                    }
                    else{
                        kitShipATP=0;
                        break;
                    }
                }
                MultiReadResponse kitmultiReadResponse = new MultiReadResponse();
                if (Objects.nonNull(location))
                    kitmultiReadResponse.setNodeType(location.getNodeType());
                if(kitShipATP!=0) {
                    Map<String, Integer> inventoryforKit = new HashMap<>();
                    //display ll the inventory
                    inventoryforKit.put(Constants.SHIP_ATP, kitShipATP);
                    inventoryforKit.put(Constants.BOPUS_ATP, 0);
                    inventoryforKit.put(IS_DIRTY, 0);
                    inventoryforKit.put(SHIP_SAFETY_FACTOR,0);
                    inventoryforKit.put(BOPUS_SAFETY_FACTOR, 0);
                    inventoryforKit.put(RESERVED_DEMAND, 0);
                    inventoryforKit.put(UNCONFIRMED_DEMAND,0);
                    inventoryforKit.put(PICK_SAFETY_FACTOR_SOURCE, 0);
                    inventoryforKit.put(ON_HAND_SUPPLY, 0);
                    inventoryforKit.put(UNCONFIRMED_DEMAND,0);
                    inventoryforKit.put(INVENTORY_TIMESTAMP_HIGH, 0);
                    inventoryforKit.put(INVENTORY_TIMESTAMP_LOW,0);
                    Inventorypicture invAva = new Inventorypicture();
                    invAva.setInvall(inventoryforKit);
                    kitmultiReadResponse.setKey(kitInventoryKey);
                    kitmultiReadResponse.setComponents(mapOfkitItemIdandComponenet.get(kitItemId));
                    kitmultiReadResponse.setBody(invAva);
                    kitmultiReadResponse.setStatus(HTTP_OK);
                    kitmultiReadResponse.setMessage(HTTP_OK_MESSAGE);
                    multiReadResponseList.add(kitmultiReadResponse);
                }
                else{
                    Inventorypicture invAva = new Inventorypicture();
                    Map<String, Integer> notfoundmap = new HashMap<String, Integer>();
                    kitmultiReadResponse.setKey(kitInventoryKey);
                    invAva.setInvall(notfoundmap);
                    kitmultiReadResponse.setBody(invAva);
                    kitmultiReadResponse.setStatus(RECORD_NOT_FOUND);
                    kitmultiReadResponse.setMessage(RECORD_NOT_FOUND_MESSAGE);
                    multiReadResponseList.add(kitmultiReadResponse);
                }
            }
            //getting all the inventory for all the componenet
            Iterator<String> kitInventoryKeys = keysforComponent.iterator();
            while (kitInventoryKeys.hasNext()) {
                String inventoryKey = kitInventoryKeys.next(); //inventoryKey is JCP|storeid|itemid|EACH
                String[] inventoryKeySplit = inventoryKey.split("\\|");

                String storeID = inventoryKeySplit[0] + Constants.SEPARATOR + inventoryKeySplit[1];
                String itemId = inventoryKey.split("\\|")[2];

                Location location = locations.get(storeID);

                Map<String, Integer> inventoryValueForOutput = componentInventory.get(inventoryKey);

                MultiReadResponse multiReadResponseforcomponent = new MultiReadResponse();
                multiReadResponseforcomponent.setKey(inventoryKey);
                if (Objects.nonNull(location))
                    multiReadResponseforcomponent.setNodeType(location.getNodeType());

                if (Objects.nonNull(inventoryValueForOutput)) {

                    int supply = 0;
                    int demand = 0;

                    if (inventoryValueForOutput.containsKey(Constants.ON_HAND_SUPPLY)) {
                        supply = inventoryValueForOutput.get(Constants.ON_HAND_SUPPLY);
                    }
                    if (inventoryValueForOutput.containsKey(Constants.UNCONFIRMED_DEMAND)) {
                        demand = demand + inventoryValueForOutput.get(Constants.UNCONFIRMED_DEMAND);
                    }
                    if (inventoryValueForOutput.containsKey(Constants.RESERVED_DEMAND)) {
                        demand = demand + inventoryValueForOutput.get(Constants.RESERVED_DEMAND);
                    }

                    inventoryValueForOutput.put(Constants.SHIP_ATP, supply - demand);
                    inventoryValueForOutput.put(Constants.BOPUS_ATP, 0);

                    Inventorypicture invAva = new Inventorypicture();
                    invAva.setInvall(inventoryValueForOutput);
                    multiReadResponseforcomponent.setBody(invAva);
                    multiReadResponseforcomponent.setStatus(HTTP_OK);
                    multiReadResponseforcomponent.setMessage(HTTP_OK_MESSAGE);
                    multiReadResponseList.add(multiReadResponseforcomponent);
                } else {
                    Inventorypicture invAva = new Inventorypicture();
                    Map<String, Integer> notfoundmap = new HashMap<String, Integer>();
                    invAva.setInvall(notfoundmap);
                    multiReadResponseforcomponent.setBody(invAva);
                    multiReadResponseforcomponent.setStatus(RECORD_NOT_FOUND);
                    multiReadResponseforcomponent.setMessage(RECORD_NOT_FOUND_MESSAGE);
                    multiReadResponseList.add(multiReadResponseforcomponent);
                }

            }
            response.setBody(multiReadResponseList);
            response.setStatus(SVC_INVOCATION_SUCCESS);
            response.setMessage(SVC_INVOCATION_SUCCESS_MESSAGE);
            return response;

        }
        //substituteAvailabilityEnabled

        Set<String> keys = new HashSet<>();
        Set<String> finalKeys = keys;
        Map<String, String> mapOfParentToSubs = new HashMap<>();

        if(Boolean.parseBoolean(event.getProperties().get(ConfigurationsEureka.FEATURE_SUBSTITUTE_AVAILABILITY))) {
            log.info("Substitute Availability Flag enabled");
            //Create inventoryKeys for substitute itemid's (if any) across all WHSE storeid's if WHSE-DG or with the WHSE storeid from request
            substituteAvailabilityMultireadResponse(itemDetails, multiReadInputForWHNodeOrWHDG, finalKeys, mapOfParentToSubs, distributionGroupMap);
        }

        //keys would be JCP|storeid|itemid|EACH
        keys = formInventoryKeys(multiReadInputForWHNodeOrWHDG, distributionGroupStores, locations, keys, isPurifyAtpRequest);
        keys.addAll(finalKeys);
        Map<String, Map<String, Integer>> inventory = getInventory(keys);
        Iterator<String> inventoryKeys = keys.iterator();

        while(inventoryKeys.hasNext()){
            String inventoryKey = inventoryKeys.next(); //inventoryKey is JCP|storeid|itemid|EACH
            String[] inventoryKeySplit = inventoryKey.split("\\|");

            String storeID = inventoryKeySplit[0] + Constants.SEPARATOR + inventoryKeySplit[1];
            String itemId = inventoryKey.split("\\|")[2];
            Location location = locations.get(storeID);
            Map<String, Integer> inventoryValueForOutput = inventory.get(inventoryKey);

            MultiReadResponse multiReadResponse = new MultiReadResponse();
            multiReadResponse.setKey(inventoryKey);
            if(Objects.nonNull(location))
                multiReadResponse.setNodeType(location.getNodeType());

            if(Objects.nonNull(inventoryValueForOutput)) {

                int supply = 0;
                int demand = 0;

                if (inventoryValueForOutput.containsKey(Constants.ON_HAND_SUPPLY)) {
                    supply = inventoryValueForOutput.get(Constants.ON_HAND_SUPPLY);
                }
                if (inventoryValueForOutput.containsKey(Constants.UNCONFIRMED_DEMAND)) {
                    demand = demand + inventoryValueForOutput.get(Constants.UNCONFIRMED_DEMAND);
                }
                if (inventoryValueForOutput.containsKey(Constants.RESERVED_DEMAND)) {
                    demand = demand + inventoryValueForOutput.get(Constants.RESERVED_DEMAND);
                }

                //set substitute atp and substitute itemid in multiread response
                if(Boolean.parseBoolean(event.getProperties().get(ConfigurationsEureka.FEATURE_SUBSTITUTE_AVAILABILITY))) {
                    setSubstituteAtpItemIdInOutput(mapOfParentToSubs, inventoryValueForOutput, itemId, storeID, inventory, multiReadResponse);
                }
                inventoryValueForOutput.put(Constants.SHIP_ATP, supply - demand);
                inventoryValueForOutput.put(Constants.BOPUS_ATP, 0);
                Inventorypicture invAva = new Inventorypicture();
                invAva.setInvall(inventoryValueForOutput);
                multiReadResponse.setBody(invAva);
                multiReadResponse.setStatus(HTTP_OK);
                multiReadResponse.setMessage(HTTP_OK_MESSAGE);
                multiReadResponseList.add(multiReadResponse);
            }
            else {
                Inventorypicture invAva = new Inventorypicture();
                Map<String, Integer> notfoundmap = new HashMap<String, Integer>();
                invAva.setInvall(notfoundmap);
                multiReadResponse.setBody(invAva);
                multiReadResponse.setStatus(RECORD_NOT_FOUND);
                multiReadResponse.setMessage(RECORD_NOT_FOUND_MESSAGE);
                multiReadResponseList.add(multiReadResponse);
            }

        }
        response.setBody(multiReadResponseList);
        response.setStatus(SVC_INVOCATION_SUCCESS);
        response.setMessage(SVC_INVOCATION_SUCCESS_MESSAGE);
        return response;
    }


    /**
     * Set corresponding substitute itemid and substitute atp in the multiread response
     * @param mapOfParentToSubs
     * @param inventoryValueForOutput
     * @param itemId
     * @param storeID
     * @param inventory
     * @param multiReadResponse
     */
    private void setSubstituteAtpItemIdInOutput(Map<String, String> mapOfParentToSubs, Map<String, Integer> inventoryValueForOutput, String itemId, String storeID, Map<String, Map<String, Integer>> inventory, MultiReadResponse multiReadResponse) {
        //if parent itemid has a substitute itemid
        if(Objects.nonNull(mapOfParentToSubs.get(itemId))){
            String subsItemId = mapOfParentToSubs.get(itemId);
            if(Objects.nonNull(subsItemId)) {
                Map<String, Integer> subsItemIdInv = inventory.get(storeID + "|" + subsItemId + "|EACH");
                if (Objects.nonNull(subsItemIdInv)) {
                    inventoryValueForOutput.put("substituteAtp", subsItemIdInv.get(Constants.ON_HAND_SUPPLY) - subsItemIdInv.get(UNCONFIRMED_DEMAND) - subsItemIdInv.get(RESERVED_DEMAND));
                    multiReadResponse.setSubstituteItemId(subsItemId);
                } else {
                    inventoryValueForOutput.put("substituteAtp", 0);
                    multiReadResponse.setSubstituteItemId(subsItemId);
                }
            }
        }
        //to find the correct parent itemid of the substitute itemid
        else {
            String parentItemId =  mapOfParentToSubs.entrySet().stream()
                    .filter(entry -> itemId.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);
            if (Objects.nonNull(parentItemId)){
                Map<String, Integer> parentItemIdInv =  inventory.get(storeID+ "|" +parentItemId +"|EACH");
                if (Objects.nonNull(parentItemIdInv)){
                    inventoryValueForOutput.put("substituteAtp", parentItemIdInv.get(Constants.ON_HAND_SUPPLY) - parentItemIdInv.get(UNCONFIRMED_DEMAND) - parentItemIdInv.get(RESERVED_DEMAND));
                    multiReadResponse.setSubstituteItemId(parentItemId);
                }
                else{
                    inventoryValueForOutput.put("substituteAtp", 0);
                    multiReadResponse.setSubstituteItemId(parentItemId);
                }
            }
        }
    }


    /**
     * This method searches for substitute items for multiread request itemid's, store in a map of substitute items with corresponding parent itemid's and
     * create additional keys with all whse storeid's if multiread input request has distGroup WHSE-DG
     * @param itemDetails
     * @param multiReadInputForWHNodeOrWHDG
     * @param finalKeys
     * @param mapOfParentToSubs
     * @param distributionGroupMap
     */
    private void substituteAvailabilityMultireadResponse(Map<String, ItemDetails> itemDetails, List<InventoryQuerry> multiReadInputForWHNodeOrWHDG, Set<String> finalKeys, Map<String, String> mapOfParentToSubs, IMap<String, Map<Integer, List<String>>> distributionGroupMap) {
        Map<String, ItemDetails> finalItemDetails = itemDetails;
        multiReadInputForWHNodeOrWHDG.stream().forEach(invQuery -> {
                    ItemDetails itemDets = finalItemDetails.get("JCP|" +invQuery.getItemID()+ "|EACH");
                    SubstituteItem substituteItem = ItemInventoryUtil.getSubstituteItemDetails(itemDets);
                    if(Objects.nonNull(substituteItem) && Objects.equals("Preferred",substituteItem.getSubstituteRelationship().getRelationType()))
                    {
                        String subsItemId = substituteItem.getSubstituteItemId();
                        mapOfParentToSubs.put(invQuery.getItemID(), subsItemId);

                        if (Objects.nonNull(invQuery.getDistributionGroup()) && invQuery.getDistributionGroup().equals("WHSE-DG")){

                            List<String> whseStores = getStoresFromDg(Constants.JCP_WHSE_DG, new HashSet<>(), null,
                                    distributionGroupMap.get(Constants.JCP_ORG_CODE + Constants.SEPARATOR + Constants.JCP_WHSE_DG));
                            whseStores.forEach(whseStore -> finalKeys.add(whseStore+Constants.SEPARATOR+subsItemId+"|EACH"));

                        }
                        if(Objects.nonNull(invQuery.getStoreID())){
                            finalKeys.add("JCP|" +invQuery.getStoreID()+Constants.SEPARATOR+subsItemId+"|EACH");
                        }
                    }
                    if (Objects.nonNull(substituteItem) && Objects.equals("Regular",substituteItem.getSubstituteRelationship().getRelationType())){
                        String parentItemId = substituteItem.getSubstituteItemId();
                        mapOfParentToSubs.put(parentItemId, invQuery.getItemID());
                        if (Objects.nonNull(invQuery.getDistributionGroup()) && invQuery.getDistributionGroup().equals("WHSE-DG")){

                            List<String> whseStores = getStoresFromDg(Constants.JCP_WHSE_DG, new HashSet<>(), null,
                                    distributionGroupMap.get(Constants.JCP_ORG_CODE + Constants.SEPARATOR + Constants.JCP_WHSE_DG));
                            whseStores.forEach(whseStore -> finalKeys.add(whseStore+Constants.SEPARATOR+parentItemId+"|EACH"));

                        }
                        if(Objects.nonNull(invQuery.getStoreID())){
                            finalKeys.add("JCP|" +invQuery.getStoreID()+Constants.SEPARATOR+parentItemId+"|EACH");
                        }
                    }
                }
        );
    }


}

