package com.easyride.notification_service.service;

import com.easyride.notification_service.model.NotificationChannel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeMarkerTemplateServiceImplTest {

    @Mock
    private Configuration freemarkerConfig;

    private FreeMarkerTemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateService = new FreeMarkerTemplateServiceImpl(freemarkerConfig);
        ReflectionTestUtils.setField(templateService, "defaultLocaleStr", "en_US");
    }

    @Test
    void processTemplate_Success() throws Exception {
        Template mockTemplate = new Template("test", new StringReader("Hello ${name}"),
                new Configuration(Configuration.VERSION_2_3_31));

        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mockTemplate);

        Map<String, Object> model = new HashMap<>();
        model.put("name", "World");

        String result = templateService.processTemplate("welcome", NotificationChannel.EMAIL_BODY, "en_US", model);
        assertEquals("Hello World", result);
    }

    @Test
    void processTemplate_NotFound() throws Exception {
        when(freemarkerConfig.getTemplate(anyString())).thenThrow(new IOException("File not found"));

        // It should try fallback, if fallback also fails (which it might since same
        // mock), it returns error code or fallback message.
        // The implementation tries fallback locale. So we should mock that sequence or
        // just expect failure if mock throws twice.

        String result = templateService.processTemplate("welcome", NotificationChannel.EMAIL_BODY, "en_US",
                new HashMap<>());
        assertTrue(result.startsWith("Error"));
    }
}
