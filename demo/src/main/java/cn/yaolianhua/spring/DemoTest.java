package cn.yaolianhua.spring;

import cn.yaolianhua.spring.config.DemoConfig;
import cn.yaolianhua.spring.factorybean.DemoFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-05 18:44
 **/
public class DemoTest {

	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(DemoConfig.class);
		context.refresh();
		for (String singletonName : context.getDefaultListableBeanFactory().getSingletonNames()) {
			System.out.println(context.getDefaultListableBeanFactory().getSingleton(singletonName));
		}
		System.out.println("----------------------");
//		System.out.println(context.getBean(DemoFactoryBean.class));
		System.out.println(context.getBean("&demoFactoryBean"));
		System.out.println(context.getBean("demoFactoryBean"));
		System.out.println(context.getBean(DemoFactoryBean.class).getObject());
	}
}
