package cn.yaolianhua.spring.factorybean;

import cn.yaolianhua.spring.service.DemoService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-13 21:50
 **/
@Component
public class DemoFactoryBean implements FactoryBean<DemoService> {
	@Autowired
	private DemoService demoService;
	@Override
	public DemoService getObject() throws Exception {
//		return new DemoService();
		return demoService;
	}

	@Override
	public Class<?> getObjectType() {
		return DemoService.class;
	}
}
