package cn.yaolianhua.spring.listener;

import org.springframework.context.ApplicationEvent;

/**
 * @author yaolianhua789@gmail.com
 * @date 2020-09-15 14:05
 **/
public class DemoEvent extends ApplicationEvent {
	private static final long serialVersionUID = 1L;
	public DemoEvent(Object source) {
		super(source);
	}
}
