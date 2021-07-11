package com.test;

import java.util.*;
import java.util.stream.Collectors;

public class Backup {

    {
        if (Boolean.parseBoolean(event.getProperties().get(FEATURE_KITCOMPONENT_AVAILABILITY))) {
            log.info("kitComponent Availability Flag enabled");
            Map<String, Map<String, Integer>> kitComponenetMappingMap = new HashMap<String, Map<String, Integer>>();
            if (Objects.nonNull(itemDetails)) {
                List<BundleInfo> bundleInfo = new ArrayList<>();
                for (Map.Entry<String, ItemDetails> entry : itemDetails.entrySet()) {
                    if (entry.getValue().isBundle() && entry.getValue().getBundleInfo() != null) {
                        bundleInfo.add(ItemInventoryUtil.getKitComponentDetails(entry.getValue()));
                    }
                }
                if (!bundleInfo.isEmpty()) {
                    bundleInfo.stream().forEach(x -> {
                        kitComponenetMappingMap.put(x.getKitItemId(), x.getComponents());
                    });
                    Set<String> setOfKitItemId = kitComponenetMappingMap.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
                    List<String> listOfComponentId = kitComponenetMappingMap.entrySet().stream().flatMap(x -> x.getValue().entrySet().stream().map(c -> c.getKey())).collect(Collectors.toList());
                    //  "deal with the keys for store id and DISTRIBUTION GROUP- (WHSE-DG) "
                    Set<String> keysforComponent = new HashSet<>();

                    multiReadInputForWHNodeOrWHDG.forEach(invQuery -> {
                        if (Objects.nonNull(invQuery.getDistributionGroup()) && invQuery.getDistributionGroup().equals("WHSE-DG")) {
                            listOfComponentId.stream().forEach(componentItemId -> {
                                List<String> whseStores = getStoresFromDg(JCP_WHSE_DG, new HashSet<>(), null,
                                        distributionGroupMap.get(JCP_ORG_CODE + SEPARATOR + JCP_WHSE_DG));
                                whseStores.stream().forEach(whseStore -> keysforComponent.add(whseStore + SEPARATOR + componentItemId + "|EACH"));
                            });
                        } else if (invQuery.getStoreID() != null) {
                            listOfComponentId.stream().forEach(componentItemId -> {
                                keysforComponent.add("JCP|" + invQuery.getStoreID() + "|" + componentItemId + "|EACH");
                            });
                        }
                    });
                    Map<String, Map<String, Integer>> componentInventory = getInventory(keysforComponent);
                    Set<String> kitItemKeys = formInventoryKeys(multiReadInputForWHNodeOrWHDG, distributionGroupStores, locations, setOfKitItemId, isPurifyAtpRequest);
                    Iterator<String> kitItemInventoryKeys = kitItemKeys.iterator();
                    while (kitItemInventoryKeys.hasNext()) {
                        String kitInventoryKey = kitItemInventoryKeys.next();
                        String[] kitInventoryKeySplit = kitInventoryKey.split("\\|");
                        String kitStoreId = kitInventoryKeySplit[0] + Constants.SEPARATOR + kitInventoryKeySplit[1];  //storeId : JCP|9132
                        String kitItemId = kitInventoryKeySplit[2];   //kit item ID 30521900081

                        Location location = locations.get(kitStoreId);

                        int kitShipATP = 0;
                        if (kitItemId != null) {
                            if (kitComponenetMappingMap.get(kitItemId) != null) {       // two times loading map
                                for (String component : kitComponenetMappingMap.get(kitItemId).keySet()) {
                                    int qty = kitComponenetMappingMap.get(kitItemId).get(component);
                                    String key = kitStoreId + Constants.SEPARATOR + component + Constants.SEPARATOR + Constants.UOM_EACH;
                                    Map<String, Integer> componentItemInventory = componentInventory.get(key);
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

                                        int shipATPComponenet = supply - demand;
                                        int noOfkitform = shipATPComponenet / qty;

                                        if (noOfkitform == 0)
                                            break;
                                        if (kitShipATP == 0)
                                            kitShipATP = noOfkitform;

                                        if (noOfkitform < kitShipATP)
                                            kitShipATP = noOfkitform;
                                    } else {
                                        kitShipATP = 0;
                                        break;
                                    }
                                }

                                MultiReadResponse kitmultiReadResponse = new MultiReadResponse();
                                if (Objects.nonNull(location))
                                    kitmultiReadResponse.setNodeType(location.getNodeType());
                                if (kitShipATP != 0) {
                                    Map<String, Integer> inventoryforKit = new HashMap<>();
                                    //display all the inventory for kit id
                                    inventoryforKit.put(Constants.SHIP_ATP, kitShipATP);
                                    inventoryforKit.put(Constants.BOPUS_ATP, 0);
                                    inventoryforKit.put(IS_DIRTY, 0);
                                    inventoryforKit.put(SHIP_SAFETY_FACTOR, 0);
                                    inventoryforKit.put(BOPUS_SAFETY_FACTOR, 0);
                                    inventoryforKit.put(RESERVED_DEMAND, 0);
                                    inventoryforKit.put(UNCONFIRMED_DEMAND, 0);
                                    inventoryforKit.put(PICK_SAFETY_FACTOR_SOURCE, 0);
                                    inventoryforKit.put(ON_HAND_SUPPLY, 0);
                                    inventoryforKit.put(UNCONFIRMED_DEMAND, 0);
                                    inventoryforKit.put(INVENTORY_TIMESTAMP_HIGH, 0);
                                    inventoryforKit.put(INVENTORY_TIMESTAMP_LOW, 0);
                                    Inventorypicture invAva = new Inventorypicture();
                                    invAva.setInvall(inventoryforKit);
                                    kitmultiReadResponse.setKey(kitInventoryKey);
                                    kitmultiReadResponse.setComponents(kitComponenetMappingMap.get(kitItemId));
                                    kitmultiReadResponse.setBody(invAva);
                                    kitmultiReadResponse.setStatus(HTTP_OK);
                                    kitmultiReadResponse.setMessage(HTTP_OK_MESSAGE);
                                    multiReadResponseList.add(kitmultiReadResponse);
                                } else {
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
                if(multiReadInputForWHNodeOrWHDG.size() != bundleInfo.size())
            }
        }
        ++++++++++++++++date 01/06/2020


        List<InventoryQuerry> listofKitItem = new ArrayList<>();    //list contain only kit item
        itemDetails.entrySet().stream().forEach(kit -> {
            if (kit.getKey() != null && kit.getValue().isBundle() && kit.getValue().getBundleInfo() != null) {
                String[] kitInventoryKeySplit = kit.getKey().split("\\|");
                String kitItemId = kitInventoryKeySplit[1];
                multiReadInputForWHNodeOrWHDG.stream().forEach(x -> {
                    if (x.getItemID().equals(kitItemId)) {
                        listofKitItem.add(x);
                    }}); } });
        listofKitItem.forEach(x-> {                              // filter
            if(multiReadInputForWHNodeOrWHDG.contains(x)) {
                multiReadInputForWHNodeOrWHDG.remove(x);
            }
        });

        Map<String, Map<String, Integer>> kitComponenetMappingMap = new HashMap<String, Map<String, Integer>>();
        if (Objects.nonNull(itemDetails) && !listofKitItem.isEmpty()) {
            List<BundleInfo> bundleInfo = new ArrayList<>();
            itemDetails.forEach((key, value) -> {
                if (value.isBundle() && value.getBundleInfo() != null) {
                    bundleInfo.add(ItemInventoryUtil.getKitComponentDetails(value));
                }
            });
            if (!bundleInfo.isEmpty()) {
                bundleInfo.stream().forEach(x -> {
                    kitComponenetMappingMap.put(x.getKitItemId(), x.getComponents());
                });
                Set<String> setOfKitItemId = kitComponenetMappingMap.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
                List<String> listOfComponentId = kitComponenetMappingMap.entrySet().stream().flatMap(x -> x.getValue().entrySet().stream().map(c -> c.getKey())).collect(Collectors.toList());
                //  "deal with the keys for store id and DISTRIBUTION GROUP- (WHSE-DG) "
                Set<String> keysforComponent = new HashSet<>();

                listofKitItem.forEach(invQuery -> {
                    if (Objects.nonNull(invQuery.getDistributionGroup()) && invQuery.getDistributionGroup().equals("WHSE-DG")) {
                        listOfComponentId.stream().forEach(componentItemId -> {
                            List<String> whseStores = getStoresFromDg(JCP_WHSE_DG, new HashSet<>(), null,
                                    distributionGroupMap.get(JCP_ORG_CODE + SEPARATOR + JCP_WHSE_DG));
                            whseStores.stream().forEach(whseStore -> keysforComponent.add(whseStore + SEPARATOR + componentItemId + "|EACH"));
                        });
                    } else if (invQuery.getStoreID() != null) {
                        listOfComponentId.stream().forEach(componentItemId -> {
                            keysforComponent.add("JCP|" + invQuery.getStoreID() + "|" + componentItemId + "|EACH");
                        });
                    }
                });
                Map<String, Map<String, Integer>> componentInventory = getInventory(keysforComponent);
                Set<String> kitItemKeys = formInventoryKeys(listofKitItem, distributionGroupStores, locations, setOfKitItemId, isPurifyAtpRequest);
                Iterator<String> kitItemInventoryKeys = kitItemKeys.iterator();
                while (kitItemInventoryKeys.hasNext()) {
                    String kitInventoryKey = kitItemInventoryKeys.next();
                    String[] kitInventoryKeySplit = kitInventoryKey.split("\\|");
                    String kitStoreId = kitInventoryKeySplit[0] + Constants.SEPARATOR + kitInventoryKeySplit[1];  //storeId : JCP|9132
                    String kitItemId = kitInventoryKeySplit[2];   //kit item ID 30521900081

                    Location location = locations.get(kitStoreId);

                    int kitShipATP = 0;
                    if (kitItemId != null) {
                        for (String component : kitComponenetMappingMap.get(kitItemId).keySet()) {
                            int qty = kitComponenetMappingMap.get(kitItemId).get(component);
                            String key = kitStoreId + Constants.SEPARATOR + component + Constants.SEPARATOR + Constants.UOM_EACH;
                            Map<String, Integer> componentItemInventory = componentInventory.get(key);
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

                                int shipATPComponenet = supply - demand;
                                int noOfkitform = shipATPComponenet / qty;

                                if (noOfkitform == 0)
                                    break;
                                if (kitShipATP == 0)
                                    kitShipATP = noOfkitform;

                                if (noOfkitform < kitShipATP)
                                    kitShipATP = noOfkitform;
                            } else {
                                kitShipATP = 0;
                                break;
                            }
                        }

                        MultiReadResponse kitmultiReadResponse = new MultiReadResponse();
                        if (Objects.nonNull(location))
                            kitmultiReadResponse.setNodeType(location.getNodeType());
                        if (kitShipATP != 0) {
                            Map<String, Integer> inventoryforKit = new HashMap<>();  //invforKitComponent
                            //display all the inventory for kit id
                            inventoryforKit.put(Constants.SHIP_ATP, kitShipATP);
                            inventoryforKit.put(Constants.BOPUS_ATP, 0);
                            inventoryforKit.put(IS_DIRTY, 0);
                            inventoryforKit.put(SHIP_SAFETY_FACTOR, 0);
                            inventoryforKit.put(BOPUS_SAFETY_FACTOR, 0);
                            inventoryforKit.put(RESERVED_DEMAND, 0);
                            inventoryforKit.put(UNCONFIRMED_DEMAND, 0);
                            inventoryforKit.put(PICK_SAFETY_FACTOR_SOURCE, 0);
                            inventoryforKit.put(ON_HAND_SUPPLY, 0);
                            inventoryforKit.put(UNCONFIRMED_DEMAND, 0);
                            inventoryforKit.put(INVENTORY_TIMESTAMP_HIGH, 0);
                            inventoryforKit.put(INVENTORY_TIMESTAMP_LOW, 0);
                            Inventorypicture invAva = new Inventorypicture();
                            invAva.setInvall(inventoryforKit);
                            kitmultiReadResponse.setKey(kitInventoryKey);
                            kitmultiReadResponse.setComponents(kitComponenetMappingMap.get(kitItemId));
                            kitmultiReadResponse.setBody(invAva);
                            kitmultiReadResponse.setStatus(HTTP_OK);
                            kitmultiReadResponse.setMessage(HTTP_OK_MESSAGE);
                            multiReadResponseList.add(kitmultiReadResponse);
                        } else {
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
            }
        }