package syudy.querydsl.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        final Team teamA = new Team("Team-A");
        final Team teamB = new Team("Team-B");
        em.persist(teamA);
        em.persist(teamB);

        final Member member1 = new Member("DOBY", 31, teamA);
        final Member member2 = new Member("RED", 33, teamA);
        final Member member3 = new Member("LIME", 28, teamB);
        final Member member4 = new Member("NORI", 30, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Member 객체 삽입 후 조회")
    void test_select_01() {
       final List<Member> members = em.createQuery("select m from Member m", Member.class)
                                      .getResultList();

       assertEquals(4, members.size());
    }
}