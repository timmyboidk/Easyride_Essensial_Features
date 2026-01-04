# External APIs and Keys Required for EasyRide

This document lists all external APIs and API keys required to run the EasyRide application in production mode (non-mocked environment).

## 📋 Summary Table

| Service | Provider | Purpose | Priority | Estimated Cost |
|---------|----------|---------|----------|----------------|
| Google Maps API | Google Cloud Platform | Location, routing, geocoding | **Critical** | Pay-as-you-go |
| Stripe API | Stripe | Payment processing | **Critical** | Transaction fees |
| PayPal API | PayPal | Payment processing | High | Transaction fees |
| AWS SNS | Amazon Web Services | SMS notifications | High | Pay-per-SMS |
| Twilio API | Twilio | SMS notifications (alternative) | Medium | Pay-per-SMS |
| Firebase FCM | Google Firebase | Android push notifications | High | Free |
| Apple APNs | Apple Developer | iOS push notifications | High | $99/year |
| SMTP Server | Various providers | Email notifications | Medium | Varies |
| OAuth2/JWT | Self-hosted or Auth0 | Authentication & authorization | **Critical** | Free (self) or paid |

---

## 🗺️ 1. Google Maps API

**Service:** `location_service`  
**Provider:** Google Cloud Platform  
**Documentation:** https://developers.google.com/maps/documentation

### Required APIs to Enable:
- **Maps JavaScript API** - For map display
- **Directions API** - For route calculation
- **Distance Matrix API** - For distance/duration estimates
- **Geocoding API** - For address to coordinates conversion
- **Places API** - For location search and autocomplete
- **Geolocation API** - For device location

### Configuration Location:
```yaml
# location_service/src/main/resources/application.yml
google:
  maps:
    api:
      key: YOUR_GOOGLE_MAPS_API_KEY_HERE
```

### How to Obtain:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable the required APIs listed above
4. Go to "Credentials" → Create credentials → API key
5. Restrict the API key to specific APIs for security
6. Add billing information (required for production use)

### Security Recommendations:
- ✅ Restrict API key to specific APIs
- ✅ Restrict to specific IP addresses (backend server)
- ✅ Set usage quotas to prevent unexpected charges
- ✅ Use environment variables instead of hardcoding

### Estimated Cost:
- **Free Tier:** $200 credit/month
- **Pricing:** ~$5-7 per 1,000 requests (varies by API)
- **Typical monthly cost:** $100-500 depending on usage

---

## 💳 2. Stripe API

**Service:** `payment_service`  
**Provider:** Stripe  
**Documentation:** https://stripe.com/docs/api

### What It's Used For:
- Credit card payment processing
- Creating payment intents
- Processing refunds
- Managing customer payment methods

### Configuration Location:
```yaml
# payment_service/src/main/resources/application.yml
payment-gateway:
  stripe:
    api-key: sk_test_... # Test key starts with sk_test_
                         # Live key starts with sk_live_
```

### How to Obtain:
1. Sign up at [Stripe Dashboard](https://dashboard.stripe.com/register)
2. Complete business verification
3. Go to Developers → API keys
4. Copy the **Secret key** (starts with `sk_test_` for testing, `sk_live_` for production)
5. Also note the **Publishable key** for frontend integration

### API Keys Needed:
- **Secret Key** (Backend): `sk_test_...` or `sk_live_...`
- **Publishable Key** (Frontend/Mobile): `pk_test_...` or `pk_live_...`

### Security Recommendations:
- ✅ NEVER expose secret key in frontend code
- ✅ Use test keys during development
- ✅ Store keys in environment variables
- ✅ Enable webhook signature verification
- ✅ Use restricted API keys when possible

### Estimated Cost:
- **No monthly fee** for standard accounts
- **Transaction fee:** 2.9% + $0.30 per successful charge
- **International cards:** Additional 1.5%
- **Example:** For $1,000 in transactions = ~$29 in fees

---

## 💰 3. PayPal API

**Service:** `payment_service`  
**Provider:** PayPal  
**Documentation:** https://developer.paypal.com/docs/api/overview/

### What It's Used For:
- PayPal account payments
- Alternative payment method
- Wallet withdrawals

### Configuration Location:
```yaml
# payment_service/src/main/resources/application.yml
payment-gateway:
  paypal:
    client-id: YOUR_PAYPAL_CLIENT_ID
    client-secret: YOUR_PAYPAL_CLIENT_SECRET
    mode: sandbox # or 'live' for production
```

### How to Obtain:
1. Sign up at [PayPal Developer](https://developer.paypal.com/)
2. Go to Dashboard → My Apps & Credentials
3. Create a REST API app
4. Copy the **Client ID** and **Secret**
5. Use sandbox credentials for testing

### API Keys Needed:
- **Client ID**: Public identifier
- **Client Secret**: Private key for authentication

### Security Recommendations:
- ✅ Keep client secret secure
- ✅ Use sandbox mode for development
- ✅ Validate webhook signatures
- ✅ Use environment variables

### Estimated Cost:
- **Transaction fee:** 2.9% + $0.30 per transaction (US)
- **International:** Additional 1.5%-4.4%
- **Example:** For $1,000 in transactions = ~$29 in fees

---

## 📱 4. AWS SNS (Simple Notification Service)

**Service:** `notification_service`  
**Provider:** Amazon Web Services  
**Documentation:** https://docs.aws.amazon.com/sns/

### What It's Used For:
- Sending SMS messages to users
- OTP verification codes
- Order notifications
- Alert messages

### Configuration Location:
```properties
# notification_service/src/main/resources/application.properties
# AWS credentials should be in ~/.aws/credentials or environment variables
aws.sns.phone-number-prefix=+1
```

### How to Obtain:
1. Create an [AWS Account](https://aws.amazon.com/)
2. Go to IAM → Create a new user for programmatic access
3. Attach policy: `AmazonSNSFullAccess` (or create custom restricted policy)
4. Save the **Access Key ID** and **Secret Access Key**
5. Configure AWS CLI or use environment variables:
   ```bash
   export AWS_ACCESS_KEY_ID=your_access_key
   export AWS_SECRET_ACCESS_KEY=your_secret_key
   export AWS_DEFAULT_REGION=us-east-1
   ```

### Required Credentials:
- **AWS Access Key ID**
- **AWS Secret Access Key**
- **AWS Region** (e.g., us-east-1, eu-west-1)

### Security Recommendations:
- ✅ Use IAM user with minimal required permissions
- ✅ Never commit credentials to version control
- ✅ Enable MFA for AWS account
- ✅ Rotate keys regularly
- ✅ Use AWS Secrets Manager for production

### Estimated Cost:
- **SMS (US):** $0.00645 per message
- **SMS (International):** $0.02-0.60 per message depending on country
- **Free Tier:** First 1,000 messages free (may vary by region)
- **Example:** 10,000 SMS/month in US = ~$65/month

---

## 📞 5. Twilio API (Alternative SMS)

**Service:** `notification_service`  
**Provider:** Twilio  
**Documentation:** https://www.twilio.com/docs/sms

### What It's Used For:
- Alternative to AWS SNS
- SMS verification codes
- Notifications via SMS

### Configuration Location:
```properties
# notification_service/src/main/resources/application.properties
twilio.account.sid=YOUR_ACCOUNT_SID
twilio.auth.token=YOUR_AUTH_TOKEN
twilio.phone.number=YOUR_TWILIO_PHONE_NUMBER
```

### How to Obtain:
1. Sign up at [Twilio Console](https://www.twilio.com/console)
2. Verify your account and add billing
3. Get a phone number from Twilio
4. Copy your **Account SID** and **Auth Token** from the dashboard

### Required Credentials:
- **Account SID**: Account identifier
- **Auth Token**: Authentication token
- **Phone Number**: Twilio phone number for sending SMS

### Security Recommendations:
- ✅ Keep auth token secure
- ✅ Use environment variables
- ✅ Enable webhook authentication
- ✅ Monitor usage to prevent fraud

### Estimated Cost:
- **Phone number:** $1.00-$2.00/month
- **SMS (US):** $0.0075 per message
- **SMS (International):** $0.05-$0.50 per message
- **Free Trial:** $15 credit (with limitations)
- **Example:** 10,000 SMS/month in US = ~$75/month (+ phone rental)

---

## 🔔 6. Firebase Cloud Messaging (FCM)

**Service:** `notification_service`  
**Provider:** Google Firebase  
**Documentation:** https://firebase.google.com/docs/cloud-messaging

### What It's Used For:
- Push notifications to Android devices
- Real-time order updates
- Driver assignment alerts

### Configuration Location:
```properties
# notification_service/src/main/resources/application.properties
fcm.service-account.file=firebase-service-account-key.json
```

### How to Obtain:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing
3. Go to Project Settings → Service Accounts
4. Click "Generate new private key"
5. Download the JSON file
6. Place it in `notification_service/src/main/resources/`
7. Update the path in application.properties

### Required File:
- **Service Account JSON**: Contains private key and credentials

### Security Recommendations:
- ✅ Never commit service account JSON to version control
- ✅ Add `*service-account*.json` to .gitignore
- ✅ Use environment variables for file path in production
- ✅ Store in secure secrets manager (AWS Secrets Manager, HashiCorp Vault)
- ✅ Restrict permissions on the file (chmod 600)

### Estimated Cost:
- **FREE** - No cost for FCM messaging
- Unlimited notifications

---

## 🍎 7. Apple Push Notification Service (APNs)

**Service:** `notification_service`  
**Provider:** Apple  
**Documentation:** https://developer.apple.com/documentation/usernotifications

### What It's Used For:
- Push notifications to iOS devices
- Real-time updates for iPhone/iPad users

### Configuration Location:
```properties
# notification_service/src/main/resources/application.properties
apns.bundleId=com.easyride.app
apns.teamId=YOUR_TEAM_ID
apns.keyId=YOUR_KEY_ID
apns.authKey.path=AuthKey_XXXXXXXXXX.p8
apns.environment=sandbox # or 'production'
```

### How to Obtain:
1. Enroll in [Apple Developer Program](https://developer.apple.com/programs/) ($99/year)
2. Go to Certificates, Identifiers & Profiles
3. Create an App ID with Push Notifications capability
4. Generate an APNs Authentication Key:
   - Go to Keys → Create a new key
   - Enable "Apple Push Notifications service (APNs)"
   - Download the `.p8` file (can only download once!)
   - Note the **Key ID**
5. Find your **Team ID** in Membership section

### Required Credentials:
- **Team ID**: 10-character alphanumeric
- **Key ID**: 10-character alphanumeric
- **Auth Key (.p8 file)**: Private key for APNs
- **Bundle ID**: Your app's bundle identifier

### Security Recommendations:
- ✅ Keep .p8 file secure and backed up (cannot re-download)
- ✅ Never commit to version control
- ✅ Use environment variables for sensitive data
- ✅ Use sandbox environment for testing

### Estimated Cost:
- **Apple Developer Program:** $99/year (required)
- **Push notifications:** FREE (unlimited)

---

## 📧 8. Email Service (SMTP)

**Service:** `notification_service`  
**Provider:** Various (Gmail, SendGrid, Amazon SES, Mailgun)  
**Documentation:** Varies by provider

### What It's Used For:
- Email notifications
- Receipts and invoices
- Password reset emails
- Marketing/promotional emails

### Configuration Location:
```properties
# notification_service/src/main/resources/application.properties
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your_email@example.com
spring.mail.password=your_email_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Popular Options:

#### Option A: Gmail SMTP (Development/Small Scale)
- **Host:** smtp.gmail.com
- **Port:** 587 (TLS) or 465 (SSL)
- **Security:** Enable "Less secure apps" or use App Password
- **Limit:** 500 emails/day
- **Cost:** FREE

#### Option B: SendGrid (Recommended for Production)
- **Host:** smtp.sendgrid.net
- **Port:** 587
- **Documentation:** https://sendgrid.com/docs/
- **Free Tier:** 100 emails/day forever
- **Paid:** Starting at $14.95/month for 40,000 emails
- **How to get API key:**
  1. Sign up at [SendGrid](https://signup.sendgrid.com/)
  2. Go to Settings → API Keys → Create API Key
  3. Use the API key as password with "apikey" as username

#### Option C: Amazon SES (Cost-Effective)
- **Documentation:** https://aws.amazon.com/ses/
- **Free Tier:** 62,000 emails/month (when sending from EC2)
- **Paid:** $0.10 per 1,000 emails
- **Requires:** AWS account and verification

#### Option D: Mailgun
- **Host:** smtp.mailgun.org
- **Free Tier:** 1,000 emails/month
- **Paid:** Starting at $35/month for 50,000 emails

### Security Recommendations:
- ✅ Use TLS/SSL encryption
- ✅ Use app-specific passwords (not main account password)
- ✅ Enable SPF, DKIM, and DMARC records for production
- ✅ Store credentials in environment variables
- ✅ Monitor for bounce rates and spam complaints

---

## 🔐 9. Authentication & Authorization

**Service:** Multiple services  
**Provider:** Self-hosted or Auth0/Okta  
**Documentation:** https://jwt.io/

### What It's Used For:
- User authentication
- API authorization
- Session management
- Role-based access control

### Configuration Location:

#### JWT Secret (Self-Hosted):
```yaml
# Multiple services: user_service, location_service, order_service, analytics_service
jwt:
  secret: your_very_long_jwt_secret_key_that_is_at_least_32_characters_long
  expiration: 604800000  # 7 days in milliseconds
```

#### OAuth2 (If using external provider):
```yaml
# payment_service/src/main/resources/application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-issuer.com/
```

### Option A: Self-Hosted JWT (Current Implementation)

**How to Generate Strong Secret:**
```bash
# Generate a secure random secret
openssl rand -base64 64
# Or
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

**Requirements:**
- Minimum 256 bits (32 characters) for HS256
- Should be cryptographically random
- Same secret across all services for token validation

**Cost:** FREE

### Option B: Auth0 (Managed Service)

1. Sign up at [Auth0](https://auth0.com/)
2. Create an application
3. Get **Domain** and **Client ID**
4. Configure issuer URI: `https://YOUR_DOMAIN.auth0.com/`

**Cost:**
- **Free Tier:** 7,000 active users
- **Paid:** Starting at $23/month

### Option C: Okta

1. Sign up at [Okta](https://developer.okta.com/)
2. Create an application
3. Configure OAuth2 settings

**Cost:**
- **Free Tier:** 1,000 monthly active users
- **Paid:** Custom pricing

### Security Recommendations:
- ✅ Use strong, random secrets (minimum 256 bits)
- ✅ Rotate secrets periodically
- ✅ Never commit secrets to version control
- ✅ Use environment variables
- ✅ Implement token refresh mechanism
- ✅ Set appropriate expiration times
- ✅ Use HTTPS only in production

---

## 🔧 Environment Variables Setup Guide

For production deployment, use environment variables instead of hardcoding values:

### Create `.env` file (DO NOT COMMIT):

```bash
# Google Maps
GOOGLE_MAPS_API_KEY=AIzaSy...

# Stripe
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...

# PayPal
PAYPAL_CLIENT_ID=...
PAYPAL_CLIENT_SECRET=...
PAYPAL_MODE=live

# AWS SNS
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_DEFAULT_REGION=us-east-1

# Twilio (if using)
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_PHONE_NUMBER=+1...

# Firebase FCM
FCM_SERVICE_ACCOUNT_PATH=/secure/path/to/firebase-key.json

# Apple APNs
APNS_TEAM_ID=...
APNS_KEY_ID=...
APNS_AUTH_KEY_PATH=/secure/path/to/AuthKey.p8
APNS_BUNDLE_ID=com.easyride.app

# Email (SendGrid example)
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=SG.…

# JWT
JWT_SECRET=your_very_long_random_secret_key_...

# OAuth2 (if using)
OAUTH2_ISSUER_URI=https://your-auth-provider.com/

# Database passwords (production)
DB_PASSWORD_USER=...
DB_PASSWORD_ORDER=...
DB_PASSWORD_PAYMENT=...
DB_PASSWORD_LOCATION=...
```

### Update Spring Boot to use environment variables:

```yaml
google:
  maps:
    api:
      key: ${GOOGLE_MAPS_API_KEY}

payment-gateway:
  stripe:
    api-key: ${STRIPE_SECRET_KEY}
  paypal:
    client-id: ${PAYPAL_CLIENT_ID}
    client-secret: ${PAYPAL_CLIENT_SECRET}
    mode: ${PAYPAL_MODE:sandbox}

jwt:
  secret: ${JWT_SECRET}
```

---

## 📊 Cost Summary Estimation

### Monthly Cost Breakdown (Based on 10,000 rides/month):

| Service | Estimated Monthly Cost | Notes |
|---------|----------------------|-------|
| Google Maps API | $200 - $500 | Depends on usage, $200 credit included |
| Stripe | Transaction-based | 2.9% + $0.30 per transaction |
| PayPal | Transaction-based | 2.9% + $0.30 per transaction |
| AWS SNS | $65 | ~10,000 SMS at $0.00645 each |
| Twilio | $75 + $1-2 | Alternative to AWS SNS |
| Firebase FCM | $0 | FREE |
| Apple APNs | $8.25 | $99/year = ~$8.25/month |
| SendGrid Email | $15 - $35 | Depends on volume |
| **TOTAL (estimate)** | **$350 - $700/month** | Excluding transaction fees |

### Transaction Fees (Example with $50 average ride):
- 10,000 rides × $50 = $500,000 volume
- Stripe fees: ~$14,500 (2.9% + $0.30)
- PayPal fees: ~$14,500 (2.9% + $0.30)

---

## ⚡ Quick Start Checklist

### Development Phase:
- [ ] **Google Maps:** Get API key, enable required APIs
- [ ] **Stripe:** Get test API key (sk_test_...)
- [ ] **PayPal:** Get sandbox credentials
- [ ] **JWT:** Generate strong random secret
- [ ] **Gmail SMTP:** Use for email testing
- [ ] **Firebase FCM:** Download service account JSON
- [ ] **AWS SNS / Twilio:** Optional - can mock for now

### Production Phase:
- [ ] **Google Maps:** Get production key with restrictions
- [ ] **Stripe:** Get live API key (sk_live_...)
- [ ] **PayPal:** Get live credentials
- [ ] **AWS SNS / Twilio:** Set up production account
- [ ] **SendGrid / SES:** Set up production email
- [ ] **Firebase FCM:** Production service account
- [ ] **Apple APNs:** Enroll in Developer Program, get .p8 key
- [ ] **JWT:** Generate production secret, store in secrets manager
- [ ] **SSL Certificates:** For HTTPS
- [ ] **Environment Variables:** Configure all services
- [ ] **Secrets Manager:** Use AWS Secrets Manager or HashiCorp Vault

---

## 🔒 Security Best Practices

1. **Never commit credentials to Git**
   - Add all credential files and .env to .gitignore
   - Use `.gitignore` templates for sensitive files

2. **Use environment variables**
   - Production should use secrets managers (AWS Secrets Manager, HashiCorp Vault)
   - Local development can use .env files

3. **Rotate keys regularly**
   - API keys should be rotated every 90 days
   - JWT secrets should be rotated with proper migration

4. **Restrict API keys**
   - Limit Google Maps key to specific APIs and IPs
   - Use restricted Stripe API keys when possible

5. **Monitor usage**
   - Set up billing alerts for all cloud services
   - Monitor for unusual patterns that might indicate key compromise

6. **Use HTTPS only**
   - All API calls must be over HTTPS in production
   - Configure SSL/TLS certificates

---

## 📞 Support Resources

- **Google Maps:** https://developers.google.com/maps/support
- **Stripe:** https://support.stripe.com/
- **PayPal:** https://developer.paypal.com/support/
- **AWS:** https://aws.amazon.com/support/
- **Twilio:** https://support.twilio.com/
- **Firebase:** https://firebase.google.com/support
- **Apple:** https://developer.apple.com/support/

---

## 📝 Notes

- This list is based on the current codebase analysis
- Costs are estimates and may vary based on actual usage
- Some APIs have free tiers suitable for development and testing
- Always use test/sandbox credentials during development
- Production credentials should only be deployed to production environments

**Last Updated:** January 2026
