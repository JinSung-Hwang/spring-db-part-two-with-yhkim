package hello.itemservice.repository.mybatis;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper // note: @Mapper가 있어야 MyBatis가 인식할 수 있다. 구현체는 MyBatis가 만들어준다. 만들어진 객체는 Bean등록을 진행한다. 따라서 ItemMapper는 주입받아서 사용할 수 있다.
public interface ItemMapper {
  void save(Item item);
  void update(@Param("id") Long i, @Param("updateParam")ItemUpdateDto updateParam);
  List<Item> findAll(ItemSearchCond itemSearch);
  Optional<Item> findById(Long id);
}