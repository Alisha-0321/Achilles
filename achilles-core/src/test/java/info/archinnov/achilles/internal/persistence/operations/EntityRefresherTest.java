package info.archinnov.achilles.internal.persistence.operations;

import static org.mockito.Mockito.*;
import info.archinnov.achilles.internal.context.PersistenceContext;
import info.archinnov.achilles.internal.persistence.metadata.EntityMeta;
import info.archinnov.achilles.internal.persistence.metadata.PropertyMeta;
import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.internal.helper.EntityIntrospector;
import info.archinnov.achilles.internal.proxy.EntityInterceptor;
import info.archinnov.achilles.test.builders.CompleteBeanTestBuilder;
import info.archinnov.achilles.test.mapping.entity.CompleteBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityRefresherTest {

	@InjectMocks
	private EntityRefresher entityRefresher;

	@Mock
	private EntityIntrospector introspector;

	@Mock
	private EntityProxifier proxifier;

	@Mock
	private EntityLoader loader;

	@Mock
	private EntityMeta entityMeta;

	@Mock
	private EntityInterceptor<CompleteBean> jpaEntityInterceptor;

	@Mock
	private Map<Method, PropertyMeta> dirtyMap;

	@Mock
	private Set<Method> alreadyLoaded;

	@Mock
	private PersistenceContext context;

	@Test
	public void should_refresh() throws Exception {
		CompleteBean bean = CompleteBeanTestBuilder.builder().id(12L).buid();

		when(context.<CompleteBean> getEntityClass()).thenReturn(CompleteBean.class);
		when(context.getPrimaryKey()).thenReturn(bean.getId());
		when(context.getEntity()).thenReturn(bean);

		when(proxifier.getInterceptor(bean)).thenReturn(jpaEntityInterceptor);

		when(jpaEntityInterceptor.getTarget()).thenReturn(bean);
		when(jpaEntityInterceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(context.getEntityMeta()).thenReturn(entityMeta);
		when(loader.load(context, CompleteBean.class)).thenReturn(bean);

		entityRefresher.refresh(bean,context);

		verify(dirtyMap).clear();
		verify(alreadyLoaded).clear();
		verify(jpaEntityInterceptor).setTarget(bean);
	}

	@Test(expected = AchillesStaleObjectStateException.class)
	public void should_throw_exception_when_object_staled() throws Exception {
		CompleteBean bean = CompleteBeanTestBuilder.builder().id(12L).buid();

		when(context.<CompleteBean> getEntityClass()).thenReturn(CompleteBean.class);
		when(context.getPrimaryKey()).thenReturn(bean.getId());
		when(context.getEntity()).thenReturn(bean);

		when(proxifier.getInterceptor(bean)).thenReturn(jpaEntityInterceptor);

		when(jpaEntityInterceptor.getTarget()).thenReturn(bean);
		when(jpaEntityInterceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(context.getEntityMeta()).thenReturn(entityMeta);
		when(loader.load(context, CompleteBean.class)).thenReturn(null);

		entityRefresher.refresh(bean,context);
	}
}
