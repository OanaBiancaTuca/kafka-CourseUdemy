package com.appsdeveloperblog.ws.emailnotification.handler;

import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import com.appsdeveloperblog.ws.emailnotification.error.NotRetryableException;
import com.appsdeveloperblog.ws.emailnotification.error.RetryableException;
import com.appsdeveloperblog.ws.emailnotification.io.ProcessedEventEntity;
import com.appsdeveloperblog.ws.emailnotification.io.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {
    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private RestTemplate restTemplate;
    private ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }


//    @Payload annotation is used to bind method arguments to the payload of a message, and this means that the data inside
//    o9f message payload, it will be converted to object, and it will be injected to this method as method argument.
//    @Header annotation this will read message header that has a name messageId and it will assign its value to message id method argument.

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent, @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        //  if(true) throw new NotRetryableException("An error took place. No needed to consume this message again"); //in this case we will send the message to a dead letter topic
        LOGGER.info("Received a new event: " + productCreatedEvent.getTitle() + " with product id: " +
                productCreatedEvent.getProductId());

        //Check if the message was already processed before
        ProcessedEventEntity existingRecord =processedEventRepository.findByMessageId(messageId);
        if(existingRecord!=null){
            LOGGER.info("Found a duplicate message id:{}",existingRecord.getMessageId());
            return;
        }

        String theUrl = "http://localhost:8082/response/200";
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(theUrl, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                LOGGER.info("Received respon se from a remote service: " + response.getBody());
            }
        } catch (ResourceAccessException ex) {
            LOGGER.error(ex.getMessage());
            throw new RetryableException(ex);
        } catch (HttpServerErrorException ex) {
            LOGGER.error(ex.getMessage());
            throw new NotRetryableException(ex);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            throw new NotRetryableException(ex);
        }

        try {
            //save a unique message id in a database table
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException ex) {
            throw new NotRetryableException(ex);
        }
    }

}
