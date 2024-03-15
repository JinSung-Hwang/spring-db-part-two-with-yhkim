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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * note: JdbcTemplateItemRepository에서는 sql에 파라미터를 넣을때 ? 를 이용해서 넣기 때문에 순서가 중요했다.
 * note: 만약 순서가 잘못되면 엉뚱한 칼럼에 다른 값이 들어갈 수 있다.
 * note: 하여 jdbcTemplate을 사용하는것이 아니라 NamedParameterJdbcTemplate을 사용한다.
 * note: 그러면 SQL에 파라미터를 넣을때 ? 가 아니라 파라미터 이름을 설정할 수 있다.
 * note: 또한 파라미터도 array가 아니라 SqlParameterSource의 구현체들을 사용하든 Map을 사용할 수 있다.
 * note: 마지막으로 BeanPropertyRowMapper를 사용하면 맵퍼를 구현하지 않아고 클래스 타입만 넣어주면 SQL결과에 맞춰서 camelCase를 변환해준다.
 *
 * note: NamedParameterJdbcTemplate
 * note: SqlParameterSource
 * note:  - BeanPropertySqlParameterSource
 * note:  - MapSqlParameterSource
 * note: Map
 *
 * note: BeanPropertyRowMapper
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

  //  private final JdbcTemplate template;
  private final NamedParameterJdbcTemplate template;

  public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
    this.template = new NamedParameterJdbcTemplate(dataSource);
  }


  @Override
  public Item save(Item item) {
    String sql = "insert into item(item_name, price, quantity) values(:itemName, :price, :quantity)";

    BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(item); // note: 이 방법을 사용하면 객체만 넣으면 SQL에 넣을 파라미터로 변환해주기때문에 가장 편하다.
    // note: 하지만 update메서드의 SQL처럼 객체 파라미터와 ID가 분리되어있는 경우에는 사용할 수 없어서 MapSqlParameterSource이나 Map을 사용해야한다.

    KeyHolder keyHolder = new GeneratedKeyHolder();
    template.update(sql, param, keyHolder);

    long key = keyHolder.getKey().longValue();
    item.setId(key);
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
