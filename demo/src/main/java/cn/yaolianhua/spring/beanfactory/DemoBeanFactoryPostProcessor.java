package cn.yaolianhua.spring.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-12 14:59
 **/
@Component
public class DemoBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println(this.getClass().getSimpleName() + " postProcessBeanFactory(beanFactory)");
	}
}
