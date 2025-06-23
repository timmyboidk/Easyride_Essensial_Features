# Notification Service

The Notification Service is responsible for handling all outbound communications to users (passengers and drivers) of the EasyRide platform. It supports multiple channels including SMS, Email, and Push Notifications (APNs for iOS, FCM for Android).

## Core Functionality

- **Event-Driven**: Listens to business events published by other microservices (e.g., Order Service, Payment Service, User Service) via RocketMQ.
- **Multi-Channel Delivery**: Dispatches notifications through the appropriate channel(s) based on event type and user preferences.
    - **SMS**: Uses AWS SNS.
    - **Email**: Uses JavaMail with a configured SMTP server.
    - **Push Notifications**:
        - Apple Push Notification Service (APNs) via Pushy library for iOS devices.
        - Firebase Cloud Messaging (FCM) via Firebase Admin SDK for Android devices.
- **Templating**: Utilizes a templating engine (e.g., FreeMarker) to generate localized and dynamic notification content. Templates are organized by channel (sms, email, push) and locale.
- **User Preference Aware (Future)**: Designed to eventually incorporate user-defined notification preferences (though preference fetching logic from User Service is not yet fully implemented in the event DTOs).

## Configuration

Key configurations are managed in `application.properties`:

- `aws.region`: For AWS SNS.
- `apns.*`: Configuration for Pushy APNs client.
- `spring.mail.*`: SMTP server details for email.
- `fcm.service-account.file`: Path to the Firebase service account key JSON for FCM.
- `notification.templates.base-path`: Base directory for notification templates.
- `notification.templates.default-locale`: Default locale for templates.
- `rocketmq.name-server`: RocketMQ nameserver address.
- `rocketmq.consumer.group`: Default consumer group for RocketMQ listeners.

AWS credentials for SNS and Firebase service account key for FCM should be managed securely (e.g., via environment variables or a secrets management system).

## Event Consumption

The service listens to the following topics and tags (examples):

- **`order-topic`**:
    - `ORDER_ACCEPTED`
    - `DRIVER_ARRIVED`
    - `ORDER_COMPLETED`
    - `ORDER_CANCELLED_PASSENGER`
    - `ORDER_SCHEDULED_REMINDER`
- **`payment-topic`**:
    - `PAYMENT_SUCCESSFUL`
    - `PAYMENT_FAILED`
    - `REFUND_PROCESSED`
- **`user-topic`**:
    - `USER_WELCOME` (after registration)
    - `PASSWORD_RESET_OTP`
    - `ACCOUNT_VERIFIED`
    - `DRIVER_APPLICATION_APPROVED`
    - `DRIVER_APPLICATION_REJECTED`
    - `ACCOUNT_SUSPENDED` (system notification)

The event DTOs (e.g., `ConsumedOrderEventDto`) are expected to carry necessary data for template rendering and user contact information (phone, email, push tokens, locale).

## How it Works

1.  A business event occurs in another service (e.g., an order is completed in Order Service).
2.  The source service publishes an event message to a specific RocketMQ topic with relevant tags and a payload (e.g., `ConsumedOrderEventDto`).
3.  Notification Service's RocketMQ listeners (e.g., `OrderEventsListener`) consume these messages.
4.  The listener passes the event data to the `NotificationDispatcherService`.
5.  The `NotificationDispatcherService` determines the appropriate template key based on the event type.
6.  It uses the `TemplateService` (e.g., `FreeMarkerTemplateServiceImpl`) to process the template with data from the event, generating content for each required channel (SMS, Email, Push title/body).
7.  The dispatcher then calls the respective channel-specific services (`SmsNotificationService`, `EmailNotificationService`, `PushNotificationService`) to send the composed notifications.

## Future Enhancements
- Integrate fetching of user notification preferences from User Service.
- More sophisticated retry mechanisms for failed notifications.
- Admin interface for managing templates and viewing notification logs.