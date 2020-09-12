/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();
		/**
		 * 注意：{@link BeanDefinitionRegistryPostProcessor}继承自{@link BeanFactoryPostProcessor}
		 *
		 * 第一步，首先调用实现{@link BeanDefinitionRegistryPostProcessor}接口的子类
		 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// BeanFactoryPostProcessor集合
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// BeanDefinitionRegistryPostProcessor集合
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			/**
			 * 注意：自己实现的子类不会出现在{@value beanFactoryPostProcessors}中！！！
			 * @see AbstractApplicationContext#getBeanFactoryPostProcessors()
			 *
			 * 情况一：
			 * 入参{@value beanFactoryPostProcessors}中处理器实现自{@link BeanDefinitionRegistryPostProcessor}
			 * 调用{@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}
			 * 并加入到registryProcessors集合中
			 *
			 * 情况二：
			 * 入参{@value beanFactoryPostProcessors}中处理器实现自{@link BeanFactoryPostProcessor}
			 * 直接加入到regularPostProcessors集合中
			 */
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 * 根据给定类型获取bean名称集[包括子类类型]
			 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
			 * @see DefaultListableBeanFactory#beanDefinitionNames
			 * 结果是 {@link org.springframework.context.annotation.ConfigurationClassPostProcessor}类型的bean name
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					/**
					 * 1. 创建单例{@link org.springframework.context.annotation.ConfigurationClassPostProcessor}
					 * 2. 加入到currentRegistryProcessors中
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//添加到待处理的BeanDefinitionRegistryPostProcessor集合中
					processedBeans.add(ppName);
				}
			}
			//排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//将currentRegistryProcessors中的处理器加入到registryProcessors中
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 调用BeanDefinitionRegistryPostProcessor后置处理
			 * {@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}
			 *
			 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)
			 * 处理相关注解,并注册相关bean(不是实例化)
			 * {@link org.springframework.context.annotation.Configuration}
			 * {@link org.springframework.context.annotation.ComponentScan}
			 * {@link org.springframework.context.annotation.ComponentScans}
			 * {@link PropertySources}
			 * {@link ImportResource}
			 * {@link org.springframework.context.annotation.Import}
			 *  @see  org.springframework.context.annotation.ImportSelector
			 *  @see  org.springframework.context.annotation.ImportBeanDefinitionRegistrar
			 * {@link org.springframework.context.annotation.Bean}
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			//清空currentRegistryProcessors
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 根据给定类型获取bean名称集[包括子类类型]
			 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
			 * @see DefaultListableBeanFactory#beanDefinitionNames 此时已经包含我们自身的业务bean
			 * 结果是 {@link org.springframework.context.annotation.ConfigurationClassPostProcessor}bean name
			 * 还有自己定义的实现了BeanDefinitionRegistryPostProcessor的bean name
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				/**
				 * ConfigurationClassPostProcessor已经处理过，所以会跳过
				 * 自己定义的实现了BeanDefinitionRegistryPostProcessor的bean同时需要实现{@link Ordered}接口才会进去
				 */
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			//排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//将currentRegistryProcessors中的处理器加入到registryProcessors中
			registryProcessors.addAll(currentRegistryProcessors);
			//调用BeanDefinitionRegistryPostProcessor后置处理，未实现Ordered接口currentRegistryProcessors则为空
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			//清空currentRegistryProcessors
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				/**
				 * 根据给定类型获取bean名称集[包括子类类型]
				 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
				 * @see DefaultListableBeanFactory#beanDefinitionNames 此时已经包含我们自身的业务bean
				 * 结果是 {@link org.springframework.context.annotation.ConfigurationClassPostProcessor}bean name
				 * 还有自己定义的实现了BeanDefinitionRegistryPostProcessor的bean name
				 */
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					//排除之前已经处理过的后置处理器
					if (!processedBeans.contains(ppName)) {
						/**
						 * 1. 创建单例[自己实现的BeanDefinitionRegistryPostProcessor后置处理器]
						 * 2. 加入到currentRegistryProcessors中
						 */
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				//排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//将currentRegistryProcessors中的处理器加入到registryProcessors中
				registryProcessors.addAll(currentRegistryProcessors);
				//调用BeanDefinitionRegistryPostProcessor后置处理[自己实现BeanDefinitionRegistryPostProcessor接口的bean]
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				//清空currentRegistryProcessors
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			/**
			 * 调用registryProcessors后置处理的{@link BeanDefinitionRegistryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}方法
			 * [之前全部调用的是{@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}方法]
			 *
			 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)
			 * 1. 将配置类({@link org.springframework.context.annotation.Configuration}注解的类)设置为代理类型(cglib)[full 配置类型，其他的为lite类型]
			 * 2. 添加一个bean的后置处理器{@link ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor}
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			/**
			 * 调用regularPostProcessors后置处理器的{@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)方法}
			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		/**
		 * 根据给定类型获取bean名称集[包括子类类型]
		 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
		 * @see DefaultListableBeanFactory#beanDefinitionNames
		 * 结果是
		 * {@link org.springframework.context.annotation.ConfigurationClassPostProcessor}类型的bean name
		 * {@link org.springframework.context.event.EventListenerMethodProcessor}bean name
		 * 还有自定义实现的bean name
		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			//排除处理过的处理器
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//实现PriorityOrdered的BeanFactoryPostProcessor后置处理器
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//实现Ordered接口的BeanFactoryPostProcessor后置处理器
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		/**
		 * 调用{@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}后置处理方法
		 * 需要实现过{@link PriorityOrdered}接口
		 */
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		/**
		 * 调用{@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}后置处理方法
		 * 需要实现过{@link Ordered}接口
		 */
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		/**
		 * 调用{@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}后置处理方法
		 * 其他未实现任何排序接口的后置处理器
		 */
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		/**
		 * 获取{@link BeanPostProcessor}类型的bean name集
		 * @see DefaultListableBeanFactory#beanDefinitionNames
		 *
		 * {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor}
		 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}
		 * 自定义的beanPostProcessor
		 */
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			/**
			 * 实现{@link PriorityOrdered}接口的beanPostProcessor
			 */
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				/**
				 * 创建{@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor}单例对象
				 * 创建{@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}单例对象
				 */
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				//将上面两个处理器加入到priorityOrderedPostProcessors中
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					//将上面两个处理器加入到priorityOrderedPostProcessors中
					internalPostProcessors.add(pp);
				}
			}
			/**
			 * 实现{@link Ordered}接口的beanPostProcessor
			 */
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//其他未实现任何排序接口的beanPostProcessor
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		/**
		 * 注册实现了{@link PriorityOrdered}接口的beanPostProcessor
		 * @see org.springframework.beans.factory.support.AbstractBeanFactory#addBeanPostProcessor(BeanPostProcessor)
		 */
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		/**
		 * 注册实现了{@link Ordered}接口的beanPostProcessor
		 * @see org.springframework.beans.factory.support.AbstractBeanFactory#addBeanPostProcessor(BeanPostProcessor)
		 */
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		/**
		 * 注册常规的beanPostProcessor
		 * @see org.springframework.beans.factory.support.AbstractBeanFactory#addBeanPostProcessor(BeanPostProcessor)
		 */
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		/**
		 * 注册internalPostProcessors
		 * @see org.springframework.beans.factory.support.AbstractBeanFactory#addBeanPostProcessor(BeanPostProcessor)
		 */
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		/**
		 * 重新注册用于将内部bean检测为ApplicationListener的后处理器，将其移到处理器链的末尾（用于拾取代理等）
		 */
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
