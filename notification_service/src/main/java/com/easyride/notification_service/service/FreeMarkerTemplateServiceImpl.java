package com.easyride.notification_service.service;

import com.easyride.notification_service.model.NotificationChannel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class FreeMarkerTemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(FreeMarkerTemplateServiceImpl.class);

    private final Configuration freemarkerConfig;

    @Value("${notification.templates.base-path}")
    private String templatesBasePath; // e.g., "classpath:/templates/notifications/" from application.properties

    @Value("${notification.templates.default-locale}")
    private String defaultLocaleStr; // e.g., "en_US"

    @Autowired
    public FreeMarkerTemplateServiceImpl(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        // Configure base path if not done globally
        // this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates/notifications/"); // Example
    }

    @Override
    public String processTemplate(String templateKey, NotificationChannel channel, String localeStr, Map<String, Object> model) {
        Locale locale = parseLocale(localeStr);
        // Construct template name: e.g., sms/order_accepted_en_US.ftl or push/order_updated_fr_FR.ftl
        // Channel subfolder helps organize: sms, email, push
        String templateFileName = String.format("%s/%s_%s.ftl",
                channel.name().toLowerCase().split("_")[0], // sms, email, push
                templateKey,
                locale.toString());
        try {
            log.debug("Attempting to load template: {}", templateFileName);
            Template template = freemarkerConfig.getTemplate(templateFileName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException e) {
            log.warn("Template file not found: {}. Falling back to default locale or generic message.", templateFileName, e);
            // Fallback to default locale if current locale template not found
            String defaultTemplateFileName = String.format("%s/%s_%s.ftl",
                    channel.name().toLowerCase().split("_")[0],
                    templateKey,
                    defaultLocaleStr);
            try {
                Template template = freemarkerConfig.getTemplate(defaultTemplateFileName);
                return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            } catch (IOException | TemplateException ex) {
                log.error("Error processing default template {} or template {} not found: ", defaultTemplateFileName, templateFileName, ex);
                return "Error: Notification template not found or failed to process."; // Fallback generic message
            }
        } catch (TemplateException e) {
            log.error("Error processing template {}: ", templateFileName, e);
            return "Error: Failed to process notification content."; // Fallback generic message
        }
    }

    private Locale parseLocale(String localeStr) {
        if (localeStr == null || localeStr.isEmpty()) {
            return Locale.forLanguageTag(defaultLocaleStr.replace("_", "-"));
        }
        try {
            return Locale.forLanguageTag(localeStr.replace("_", "-"));
        } catch (Exception e) {
            log.warn("Invalid locale string '{}', falling back to default: {}", localeStr, defaultLocaleStr);
            return Locale.forLanguageTag(defaultLocaleStr.replace("_", "-"));
        }
    }
}