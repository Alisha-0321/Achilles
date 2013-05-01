package info.archinnov.achilles.entity.parser;

import static info.archinnov.achilles.entity.type.ConsistencyLevel.*;
import static info.archinnov.achilles.serializer.SerializerUtils.LONG_SRZ;
import static javax.persistence.CascadeType.*;
import static org.fest.assertions.api.Assertions.assertThat;
import info.archinnov.achilles.annotations.ColumnFamily;
import info.archinnov.achilles.annotations.Consistency;
import info.archinnov.achilles.consistency.AchillesConfigurableConsistencyLevelPolicy;
import info.archinnov.achilles.dao.CounterDao;
import info.archinnov.achilles.entity.context.ConfigurationContext;
import info.archinnov.achilles.entity.metadata.JoinProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.parser.context.EntityParsingContext;
import info.archinnov.achilles.entity.parser.context.PropertyParsingContext;
import info.archinnov.achilles.entity.type.ConsistencyLevel;
import info.archinnov.achilles.entity.type.WideMap;
import info.archinnov.achilles.exception.AchillesBeanMappingException;
import info.archinnov.achilles.json.ObjectMapperFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import mapping.entity.CompleteBean;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import parser.entity.UserBean;
import testBuilders.PropertyMetaTestBuilder;

/**
 * JoinPropertyParserTest
 * 
 * @author DuyHai DOAN
 * 
 */
public class JoinPropertyParserTest
{
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private JoinPropertyParser parser = new JoinPropertyParser();

	private Map<PropertyMeta<?, ?>, Class<?>> joinPropertyMetaToBeFilled = new HashMap<PropertyMeta<?, ?>, Class<?>>();
	private Map<String, HConsistencyLevel> readConsistencyMap = new HashMap<String, HConsistencyLevel>();
	private Map<String, HConsistencyLevel> writeConsistencyMap = new HashMap<String, HConsistencyLevel>();
	private EntityParsingContext entityContext;
	private AchillesConfigurableConsistencyLevelPolicy configurableCLPolicy = new AchillesConfigurableConsistencyLevelPolicy(
			ONE, ConsistencyLevel.ALL, readConsistencyMap, writeConsistencyMap);
	private ConfigurationContext configContext;

	@Mock
	private Cluster cluster;

	@Mock
	private Keyspace keyspace;

	@Mock
	private ObjectMapperFactory objectMapperFactory;

	@Mock
	private CounterDao counterDao;

	@Before
	public void setUp()
	{
		joinPropertyMetaToBeFilled.clear();
		configContext = new ConfigurationContext();
		configContext.setConsistencyPolicy(configurableCLPolicy);
		configContext.setObjectMapperFactory(objectMapperFactory);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_join_simple_property() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@OneToOne(cascade =
			{
					PERSIST,
					MERGE
			})
			@JoinColumn
			private UserBean user;

			public UserBean getUser()
			{
				return user;
			}

			public void setUser(UserBean user)
			{
				this.user = user;
			}
		}

		PropertyParsingContext context = newContext(Test.class, Test.class.getDeclaredField("user"));

		PropertyMeta<Void, UserBean> meta = (PropertyMeta<Void, UserBean>) parser
				.parseJoin(context);

		assertThat(meta.type()).isEqualTo(PropertyType.JOIN_SIMPLE);
		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).contains(PERSIST, MERGE);

		assertThat((PropertyMeta<Void, UserBean>) context.getPropertyMetas().get("user")).isSameAs(
				meta);

		assertThat((Class<UserBean>) joinPropertyMetaToBeFilled.get(meta))
				.isEqualTo(UserBean.class);
	}

	@Test
	public void should_parse_join_property_no_cascade() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@JoinColumn
			private UserBean user;

			public UserBean getUser()
			{
				return user;
			}

			public void setUser(UserBean user)
			{
				this.user = user;
			}
		}
		PropertyParsingContext context = newContext(Test.class, Test.class.getDeclaredField("user"));
		PropertyMeta<?, ?> meta = parser.parseJoin(context);

		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).isEmpty();
		assertThat(context.getJoinWideMaps()).isEmpty();
	}

	@Test
	public void should_exception_when_join_simple_property_has_cascade_remove() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@ManyToOne(cascade =
			{
					PERSIST,
					REMOVE
			})
			@JoinColumn
			private UserBean user;

			public UserBean getUser()
			{
				return user;
			}

			public void setUser(UserBean user)
			{
				this.user = user;
			}

		}

		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx.expectMessage("CascadeType.REMOVE is not supported for join columns");
		PropertyParsingContext context = newContext(Test.class, Test.class.getDeclaredField("user"));
		parser.parseJoin(context);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_join_list_property() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@OneToMany(cascade =
			{
					PERSIST,
					MERGE
			})
			@JoinColumn
			private List<UserBean> users;

			public List<UserBean> getUsers()
			{
				return users;
			}

			public void setUsers(List<UserBean> users)
			{
				this.users = users;
			}
		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));

		PropertyMeta<?, ?> meta = parser.parseJoin(context);

		assertThat(meta.type()).isEqualTo(PropertyType.JOIN_LIST);
		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).contains(PERSIST, MERGE);
		assertThat(context.getJoinWideMaps()).isEmpty();
		assertThat((Class<UserBean>) joinPropertyMetaToBeFilled.get(meta))
				.isEqualTo(UserBean.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_join_set_property() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@ManyToMany(cascade =
			{
					PERSIST,
					MERGE
			})
			@JoinColumn
			private Set<UserBean> users;

			public Set<UserBean> getUsers()
			{
				return users;
			}

			public void setUsers(Set<UserBean> users)
			{
				this.users = users;
			}
		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));
		PropertyMeta<?, ?> meta = parser.parseJoin(context);

		assertThat(meta.type()).isEqualTo(PropertyType.JOIN_SET);
		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).contains(PERSIST, MERGE);
		assertThat(context.getJoinWideMaps()).isEmpty();
		assertThat((Class<UserBean>) joinPropertyMetaToBeFilled.get(meta))
				.isEqualTo(UserBean.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_join_map_property() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@ManyToOne(cascade =
			{
					PERSIST,
					REFRESH
			})
			@JoinColumn
			private Map<Integer, UserBean> users;

			public Map<Integer, UserBean> getUsers()
			{
				return users;
			}

			public void setUsers(Map<Integer, UserBean> users)
			{
				this.users = users;
			}
		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));
		PropertyMeta<?, ?> meta = parser.parseJoin(context);

		assertThat(meta.type()).isEqualTo(PropertyType.JOIN_MAP);
		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).contains(PERSIST, REFRESH);
		assertThat(context.getJoinWideMaps()).isEmpty();
		assertThat((Class<UserBean>) joinPropertyMetaToBeFilled.get(meta))
				.isEqualTo(UserBean.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_join_widemap() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@ManyToMany(cascade =
			{
					PERSIST,
					MERGE
			})
			@JoinColumn(table = "join_users_xxx")
			private WideMap<UUID, UserBean> users;

			public WideMap<UUID, UserBean> getUsers()
			{
				return users;
			}

			public void setUsers(WideMap<UUID, UserBean> users)
			{
				this.users = users;
			}

		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));
		PropertyMeta<?, ?> meta = parser.parseJoin(context);

		assertThat(meta.type()).isEqualTo(PropertyType.JOIN_WIDE_MAP);
		JoinProperties joinProperties = meta.getJoinProperties();
		assertThat(joinProperties.getCascadeTypes()).contains(PERSIST, MERGE);
		assertThat(context.getJoinWideMaps().get(meta)).isEqualTo("join_users_xxx");
		assertThat((Class<UserBean>) joinPropertyMetaToBeFilled.get(meta))
				.isEqualTo(UserBean.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_fill_external_widemap_hashmap() throws Exception
	{
		@SuppressWarnings("unused")
		@ColumnFamily
		class Test
		{
			@ManyToMany
			@JoinColumn(table = "tablename")
			private WideMap<Integer, UserBean> users;

			public WideMap<Integer, UserBean> getUsers()
			{
				return users;
			}
		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));
		PropertyMeta<Integer, UserBean> meta = (PropertyMeta<Integer, UserBean>) parser
				.parseJoin(context);

		Map<PropertyMeta<?, ?>, String> joinExternalWideMaps = context.getJoinWideMaps();
		assertThat(
				(PropertyMeta<Integer, UserBean>) joinExternalWideMaps.keySet().iterator().next())
				.isSameAs(meta);
	}

	@Test
	public void should_set_external_join_widemap_consistency_level() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			@ManyToMany
			@JoinColumn(table = "tablename")
			@Consistency(read = QUORUM, write = ConsistencyLevel.ALL)
			private WideMap<Integer, UserBean> users;

			public WideMap<Integer, UserBean> getUsers()
			{
				return users;
			}
		}
		PropertyParsingContext context = newContext(Test.class,
				Test.class.getDeclaredField("users"));
		PropertyMeta<?, ?> propertyMeta = parser.parseJoin(context);
		assertThat(propertyMeta.getReadConsistencyLevel()).isEqualTo(QUORUM);
		assertThat(propertyMeta.getWriteConsistencyLevel()).isEqualTo(ConsistencyLevel.ALL);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_parse_external_join_wide_map() throws Exception
	{

		PropertyMeta<Void, Long> idMeta = PropertyMetaTestBuilder.valueClass(Long.class).build();
		PropertyMeta<Integer, UserBean> propertyMeta = PropertyMetaTestBuilder.noClass(
				Integer.class, UserBean.class).build();

		initEntityParsingContext();

		parser.fillJoinWideMap(entityContext, idMeta, propertyMeta, "externalTableName");

		assertThat(propertyMeta.getExternalCFName()).isEqualTo("externalTableName");
		assertThat((Serializer<Long>) propertyMeta.getIdSerializer()).isEqualTo(LONG_SRZ);

		assertThat(
				(PropertyMeta<Integer, UserBean>) entityContext.getPropertyMetas().values()
						.iterator().next()).isSameAs(propertyMeta);

		assertThat(joinPropertyMetaToBeFilled).hasSize(1);
		assertThat(
				(PropertyMeta<Integer, UserBean>) joinPropertyMetaToBeFilled.keySet().iterator()
						.next()).isSameAs(propertyMeta);
	}

	private <T> PropertyParsingContext newContext(Class<T> entityClass, Field field)
	{
		entityContext = new EntityParsingContext( //
				joinPropertyMetaToBeFilled, //
				configContext, entityClass);

		PropertyParsingContext context = entityContext.newPropertyContext(field);
		context.setJoinColumn(true);

		return context;
	}

	private void initEntityParsingContext()
	{
		entityContext = new EntityParsingContext( //
				joinPropertyMetaToBeFilled, //
				configContext, CompleteBean.class);
	}
}
