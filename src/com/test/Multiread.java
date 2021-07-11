package com.test;

import java.util.*;
import java.util.stream.Collectors;

public class Multiread {


    //Kit changes
        if (Boolean.parseBoolean(propertiesConfig.getPropertyValue(KITCOMPONENT_AVAILABILITY_ENABLED))) {
        log.info("kitComponentAvailEnabled Availability Flag enabled");
        Map<String, Map<String, Integer>> mapOfkitItemIdandComponenet = new HashMap<String, Map<String, Integer>>();
        List<BundleInfo> bundleInfo1 = null;
        for (Map.Entry<String, ItemDetails> s : itemDetails.entrySet()) {
            bundleInfo1 = ItemInventoryUtil.getKitComponentDetails(s.getValue());
        }

        List<BundleInfo> bundleInfow = ItemInventoryUtil.getKitComponentDetails(itemDetails.get(items));

        List<BundleInfo> bundleInfo = itemDetails.entrySet().stream().filter(entry -> {
            return entry.getValue().isBundle() ? true : false;
        }).map(entry1 -> entry1.getValue().getBundleInfo()).flatMap(List::stream).collect(Collectors.toList());
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








}

