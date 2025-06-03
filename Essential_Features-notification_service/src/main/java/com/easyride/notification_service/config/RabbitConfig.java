package com.easyride.notification_service.config;

Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notification.exchange");
    }

    @Bean
    public Queue smsQueue() {
        return new Queue("sms.queue");
    }

    @Bean
    public Binding smsBinding(TopicExchange exchange) {
        return BindingBuilder.bind(smsQueue())
                .to(exchange)
                .with("sms.route");
    }
}