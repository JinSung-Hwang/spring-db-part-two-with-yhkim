package hello.itemservice.repository;

import hello.itemservice.domain.Item;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * note: SimpleJdbcInsert: insert를 쉽게해주는 jdbc템플릿의 한 종류이다.
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV3 implements ItemRepository {

  //  private final JdbcTemplate template;
  private final NamedParameterJdbcTemplate template;
  private final SimpleJdbcInsert jdbcInsert;

  public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
    this.template = new NamedParameterJdbcTemplate(dataSource);
    this.jdbcInsert = new SimpleJdbcInsert(dataSource)
        .withTableName("item")
        .usingGeneratedKeyColumns("id") // note: PK를 지정하면 된다.
        .usingColumns("item_name", "price", "quantity"); // note: 모든 칼럼을 사용하면 usingColumns 메서드를 생략 가능하다.
        // note: 그것이 아니고 특정 칼럼을 사용하면 명시적으로 usingColumns 메서드를 사용해야 한다.
  }


  @Override
  public Item save(Item item) {
    BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(item);
    Number key = jdbcInsert.executeAndReturnKey(param);
    item.setId(key.longValue());
    return item;
  }

  @Override
  public void update(Long itemId, ItemUpdateDto updateParam) {
    String sql = "update item set item_name = :itemName, price = :price, quantity = :quantity where id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("itemName", updateParam.getItemName())
        .addValue("price", updateParam.getPrice())
        .addValue("quantity", updateParam.getQuantity())
        .addValue("id", itemId);

    template.update(sql, params);
  }

  @Override
  public Optional<Item> findById(Long id) {
    String sql = "select id, item_name, price, quantity from item where id = :id"; // note: DB는 snake_case로 되어있어도 자바는 camelCase를 사용하는데 이때 as를 이용해서 convention을 변환할 수 있다.
    // note: 하지만 BeanPropertyRowMapper를 사용하면 자동으로 변환해준다.
    try {
      Map<String, Object> param = Map.of("id", id);
      Item item = template.queryForObject(sql, param, itemRowMapper());
      return Optional.of(item);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private RowMapper<Item> itemRowMapper() {
    return BeanPropertyRowMapper.newInstance(Item.class); // note: camelCase 변환을 지원한다.
  }

  @Override
  public List<Item> findAll(ItemSearchCond cond) {
    String itemName = cond.getItemName();
    Integer maxPrice = cond.getMaxPrice();

    BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(cond);

    String sql = "select id, item_name, price, quantity from item"; //동적 쿼리
    if (StringUtils.hasText(itemName) || maxPrice != null) {
      sql += " where";
    }
    boolean andFlag = false;
    if (StringUtils.hasText(itemName)) {
      sql += " item_name like concat('%',:itemName,'%')";
      andFlag = true;
    }
    if (maxPrice != null) {
      if (andFlag) {
        sql += " and";
      }
      sql += " price <= :maxPrice";
    }

    log.info("sql={}", sql);
    return template.query(sql, param, itemRowMapper());
  }
}
