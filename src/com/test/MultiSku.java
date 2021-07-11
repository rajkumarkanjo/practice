public void executeQuery(SourcingRequestDto modifiedSourcingRequestDto,
        SourcingResponseDto modifiedSourcingResponseDto){
        OrderDto order = modifiedSourcingRequestDto.getOrder();
        OrderResponseDetails orderResponse =modifiedSourcingResponseDto.getOrder();
        int i=-1;
        for (OrderLineItemDto orderLine : order.getOrderLineItems()) {
        InventoryOptions options = InventoryOptions.builder()
        .omsOrderNumber(order.getOmsOrderNumber())
        .omsOrderLineNumber(orderLine.getOmsOrderLineNumber())
        .omsOrderLineQuantity(orderLine.getOrderQuantity())
        .primeDlc(orderLine.getPrimeDLC())
        .skuID(orderLine.getSkuID())
        .nodeDetails(requestContext.getSkuNodeDetails())
        .build();

        //multisku reporting changes ---Request Changes
        SourcingRequestDto sourcingRequest = new SourcingRequestDto();
        OrderDto orderDto = new OrderDto();
        orderDto.setOmsOrderNumber(order.getOmsOrderNumber());
        orderDto.setCustomerOrderNumber(order.getCustomerOrderNumber());
        orderDto.setOrderDateTimeStamp(order.getOrderDateTimeStamp());

        List<OrderLineItemDto> listOfOrderlineItems = order.getOrderLineItems();
        i++;
        List<OrderLineItemDto> orderLineItemlist = new ArrayList<>();
        orderLineItemlist.add(listOfOrderlineItems.get(i));

        orderDto.setOrderLineItems(orderLineItemlist);
        sourcingRequest.setOrder(orderDto);
        sourcingRequest.setErrors(modifiedSourcingRequestDto.getErrors());

        //multisku reporting changes ---Response Changes
        SourcingResponseDto sourcingResponse = new SourcingResponseDto();

        OrderResponseDetails orderResponseDetails = new OrderResponseDetails();
        orderResponseDetails.setCustomerOrderNumber(orderResponse.getCustomerOrderNumber());
        orderResponseDetails.setOmsOrderNumber(orderResponse.getOmsOrderNumber());

        List<OrderLineItemResponseDetails> listOfOrderLineItemsforResponse = orderResponse.getOrderLineItems();

        List<OrderLineItemResponseDetails>  orderLineItemResponseDetails = new ArrayList<>();
        orderLineItemResponseDetails.add(listOfOrderLineItemsforResponse.get(i));

        orderResponseDetails.setOrderLineItems(orderLineItemResponseDetails);

        sourcingResponse.setOrder(orderResponseDetails);
        sourcingResponse.setErrors(modifiedSourcingResponseDto.getErrors());
        //multisku reporting changes ---Response Changes END ******************************************

        // using current time for update_timestamp column, must be changed when any row is updated.
        BoundStatement boundStatement = auditReportPreparedStatement
        .bind(order.getOmsOrderNumber(), ORDER_TYPE, orderLine.getOmsOrderLineNumber(),
        orderLine.getSkuID(), requestContext.getContractType(),
        dataHelper.convertToJson(sourcingRequest, "Single Line Sourcing Request"),
        dataHelper.convertToJson(sourcingResponse, "Single Line Sourcing Response"),
        dataHelper.convertToJson(requestContext.getConfigDetails(), "Config data"),
        dataHelper.convertToJson(options, "Inventory options data"),
        requestContext.getSingleLineFinalBenefit(),
        new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));

        session.execute(boundStatement.bind());

        }
        }