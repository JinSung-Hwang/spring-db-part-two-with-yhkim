package hello.itemservice;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@RequiredArgsConstructor
public class TestDataInit {

    private final ItemRepository itemRepository;

    /**
     * 확인용 초기 데이터 추가
     */
    @EventListener(ApplicationReadyEvent.class) // note: ApplicationReadyEvent 이벤트가 발생하면 initData() 메서드를 호출한다. 이 ApplicationReadyEvent는 스프링 부트가 모두(bean, aop등) 초기화되어서 실행되는 옵션이다.
    public void initData() { // note: 하지만 이 메서드는 스프링 부트가 초기화만 된다고 실행되는것은 아니고 이 객체가 bean등록이 되어있어야 실행될 수 있다. 그래서 이 객체를 ItemServiceApplication.java에 bean으로 등록해준다.
        log.info("test data init");
        itemRepository.save(new Item("itemA", 10000, 10));
        itemRepository.save(new Item("itemB", 20000, 20));
    }

}
