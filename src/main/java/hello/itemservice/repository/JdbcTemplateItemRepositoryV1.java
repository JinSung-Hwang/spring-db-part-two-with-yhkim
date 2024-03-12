package hello.itemservice.repository;

import hello.itemservice.domain.Item;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * JdbcTemplate
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV1 implements ItemRepository {

  private final JdbcTemplate template;

  public JdbcTemplateItemRepositoryV1(DataSource dataSource) {
    this.template = new JdbcTemplate(dataSource);
  }


  @Override
  public Item save(Item item) {
    String sql = "insert into item(item_name, price, quantity) values(?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();
    // note: effectiveRows는 영향을 받은 행의 수를 반환한다.
    int effectiveRows = template.update(connection -> {
      // 자동 증가 키 // note: jdbcTemplate으로 insert시 자동키 증가된 id값을 가져오기 위해서 keyHolder를 사용한다. 만약 KeyHoler를 안사용하면 다시 조회해야한다.
      PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
      ps.setString(1, item.getItemName());
      ps.setInt(2, item.getPrice());
      ps.setInt(3, item.getQuantity());
      return ps;
    }, keyHolder);

    long key = keyHolder.getKey().longValue();
    item.setId(key);
    return item;
  }

  @Override
  public void update(Long itemId, ItemUpdateDto updateParam) {
    String sql = "update item set item_name = ?, price = ?, quantity = ? where id = ?";
    template.update(sql, updateParam.getItemName(), updateParam.getPrice(), updateParam.getQuantity(), itemId);
  }

  @Override
  public Optional<Item> findById(Long id) {
    String sql = "select id, item_name, price, quantity from item where id = ?";
    try {
      Item item = template.queryForObject(sql, itemRowMapper(), id);
      return Optional.of(item);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private RowMapper<Item> itemRowMapper() { // note: JdbcTemplate에 rowMapper를 넣으면 while(result)문을 돌려서 조회된 결과를 객체로 매핑한다.
    return ((rs, rowNum) -> {
      Item item = new Item();
      item.setId(rs.getLong("id"));
      item.setItemName(rs.getString("item_name"));
      item.setPrice(rs.getInt("price"));
      item.setQuantity(rs.getInt("quantity"));
      return item;
    });
  }

  @Override
  public List<Item> findAll(ItemSearchCond cond) {
    String itemName = cond.getItemName();
    Integer maxPrice = cond.getMaxPrice();

    String sql = "select id, item_name, price, quantity from item"; //동적 쿼리
    if (StringUtils.hasText(itemName) || maxPrice != null) {
      sql += " where";
    }
    boolean andFlag = false;
    List<Object> param = new ArrayList<>();
    if (StringUtils.hasText(itemName)) {
      sql += " item_name like concat('%',?,'%')";
      param.add(itemName);
      andFlag = true;
    }
    if (maxPrice != null) {
      if (andFlag) {
        sql += " and";
      }
      sql += " price <= ?";
      param.add(maxPrice);
    }

    log.info("sql={}", sql);
    return template.query(sql, itemRowMapper(), param.toArray());
  }
}
