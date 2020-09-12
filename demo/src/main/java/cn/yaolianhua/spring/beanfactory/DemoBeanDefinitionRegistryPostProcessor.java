package cn.yaolianhua.spring.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-12 14:58
 **/
@Component
public class DemoBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println(this.getClass().getSimpleName() + " postProcessBeanDefinitionRegistry(registry)");
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println(this.getClass().getSimpleName() + " postProcessBeanFactory(beanFactory)");
	}
}
