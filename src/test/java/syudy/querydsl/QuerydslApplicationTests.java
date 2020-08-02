package syudy.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import syudy.querydsl.entity.Hello;
import syudy.querydsl.entity.QHello;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	private EntityManager em;

	@BeforeEach
	void setUp() {
		final Hello hello = new Hello();
		em.persist(hello);
	}

	@Test
	@DisplayName("Entity 의 SELECT 확인 (셋팅 검증을 위해)")
	void test_select_01() {
		final JPAQueryFactory query = new JPAQueryFactory(em);
		final QHello qHello = new QHello("hello"); // hello는 Alias

		final Hello hello = query.selectFrom(qHello)
								 .fetchOne();
		assertThat(hello.getId()).isEqualTo(1L);
	}

}
