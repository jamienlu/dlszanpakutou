package io.github.jamielu.dlszanpakutouclient.value;

import cn.kimmking.utils.FieldUtils;
import io.github.jamielu.dlszanpakutouclient.util.PlaceholderHelper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * @author jamieLu
 * @create 2024-04-20
 */
@Slf4j
public class SpringValueHandler implements BeanPostProcessor, BeanFactoryAware, ApplicationListener<EnvironmentChangeEvent> {
    private static final PlaceholderHelper helper = PlaceholderHelper.getInstance();
    private static final MultiValueMap<String, SpringValue> VALUE_HOLDER = new LinkedMultiValueMap<>();
    @Setter
    private BeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        FieldUtils.findAnnotatedField(bean.getClass(), Value.class).forEach(
                field -> {
                    log.info("### find spring value: {}", field);
                    Value value = field.getAnnotation(Value.class);
                    helper.extractPlaceholderKeys(value.value()).forEach(key -> {
                        log.info("### find spring value: {}", key);
                        SpringValue springValue = new SpringValue(bean, beanName, key, value.value(), field);
                        VALUE_HOLDER.add(key, springValue);
                    });
                });
        return bean;
    }
    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        log.info("### >> update spring value for keys: {}", event.getKeys());
        event.getKeys().forEach(key -> {
            log.info("### >> update spring value: {}", key);
            List<SpringValue> springValues = VALUE_HOLDER.get(key);
            if(springValues == null || springValues.isEmpty()) {
                return;
            }
            springValues.forEach(springValue -> {
                        log.info("### >> update spring value: {} for key {}", springValue, key);
                        try {
                            Object value = helper.resolvePropertyValue((ConfigurableBeanFactory) beanFactory,
                                    springValue.getBeanName(), springValue.getPlaceholder());
                            log.info("### >> update value: {} for holder {}", value, springValue.getPlaceholder());
                            springValue.getField().setAccessible(true);
                            springValue.getField().set(springValue.getBean(), value);
                        } catch (Exception ex) {
                            log.error("### >> update spring value error", ex);
                        }
                    }
            );
        });
    }
}
