package cn.yaolianhua.spring.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-15 14:01
 **/
@Component
public class DemoEventListener {

	@EventListener
	public void demoEvent(DemoEvent event){
		System.out.println(this.getClass().getSimpleName() + " demoEvent(" + event.getSource() + ")");
	}
}
