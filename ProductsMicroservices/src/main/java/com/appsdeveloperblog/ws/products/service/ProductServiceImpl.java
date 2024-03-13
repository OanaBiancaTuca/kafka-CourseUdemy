package com.appsdeveloperblog.ws.products.service;


import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import com.appsdeveloperblog.ws.products.rest.CreateProductRestModel;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class ProductServiceImpl implements  ProductService{
  KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    public ProductServiceImpl(KafkaTemplate<String,ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(CreateProductRestModel productRestModel)  throws Exception{

        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId,
                productRestModel.getTitle(),productRestModel.getPrice(),productRestModel.getQuantity());

//SINCRON -> we can configure the microservice to wait for acknowledgement from all Kafka brokers that the message
// is successfully stored in Kafka topic
        //because the send method will now wait for a response, the execution will be just a little bit slower,
        //but at least we have a guarantee that our message is not lost, and it is indeed persisted in Kafka topic.


        //create producer record for headers

        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                "product-created-events-topic",
                productId,
                productCreatedEvent
        );
        record.headers().add("messageId",UUID.randomUUID().toString().getBytes());




        LOGGER.info("Before publishing a ProductCreatedEvent");
        SendResult<String,ProductCreatedEvent> result=
                kafkaTemplate.send(record).get();




//        //ASYNC-- trimitere de mesaje pe Kafka asincron, nu astept sa primesc raspuns de la kafka ca mesajul a fost primit cu succes

        //asynchronously -> will not wait for acknowledgement that the message was sucessfully persisted
                //CompletableFuture is used to perform operation asynchronously and then return result of
        // that operation when it is complete

//        CompletableFuture <SendResult<String,ProductCreatedEvent>>completableFuture=
//                kafkaTemplate.send("product-created-events-topic", productId,productCreatedEvent);


//        completableFuture.whenComplete((result,exception)->{
//            if(exception!=null){
//
//                //mesajul nu a fost salvat pe kafka cu succes, avem o exceptie
//                LOGGER.error("Failed to send message "+exception.getMessage());
//
//            }else{
//                LOGGER.info("Message sent successfully : "+result.getRecordMetadata());
//            }
//        });
//        completableFuture.join();//this method will block the current thread until the future is complete, and returns the
//        //result of computation --> synchronous

        LOGGER.info("Partition: "+result.getRecordMetadata().partition());
        LOGGER.info("Topic: "+result.getRecordMetadata().topic());
        LOGGER.info("Offset: "+result.getRecordMetadata().offset());
        LOGGER.info("******** Returning product id ");
        return productId;



    }
}
