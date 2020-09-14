package cn.yaolianhua.spring.beanpost;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-12 19:40
 **/
@Component
public class DemoBeanPostProcessor implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println(this.getClass().getSimpleName() + " postProcessBeforeInitialization(bean,beanName)");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println(this.getClass().getSimpleName() + " postProcessAfterInitialization(bean,beanName)");
		return bean;
	}
}
