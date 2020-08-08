package syudy.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import syudy.querydsl.dto.MemberSearchCondition;
import syudy.querydsl.dto.MemberTeamDto;
import syudy.querydsl.entity.Member;
import syudy.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

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
        final Member member5 = new Member(null, 28, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.persist(member5);

        em.flush();
        em.clear();
    }

    @Test
    void test_findByUsername() {
        final Member member = memberRepository.findByUsername("DOBY").get(0);

        assertEquals("DOBY", member.getUsername());
        assertEquals(31, member.getAge());
    }

    @Test
    void test_search() {
        /*
            클래스 이름에 주의
                - MemberRepository : JPARepository, MemberRepositoryImpl 다중상속
                - MemberRepositoryCustom : Custorm query를 보유하는 클래스의 인터페이스
                - MemberRepositoryImpl : MemberRepositoryCustom를 구현한 구현체
        */
        final MemberSearchCondition condition = MemberSearchCondition.builder()
                                                                     .ageGoe(31)
                                                                     .ageLoe(32)
                                                                     .build();
        final List<MemberTeamDto> dto = memberRepository.search(condition);
        assertEquals("DOBY", dto.get(0).getUserName());
    }

    @Test
    void test_simpleSearch() {
        final MemberSearchCondition condition = MemberSearchCondition.builder()
                                                                     .ageGoe(20)
                                                                     .ageLoe(40)
                                                                     .build();
        final PageRequest pageable = PageRequest.of(0, 2);
        final Page<MemberTeamDto> page = memberRepository.simpleSearch(condition, pageable);
        final List<MemberTeamDto> contents = page.getContent();
        final long total = page.getTotalElements();

        assertEquals(2, contents.size());
        assertEquals("DOBY", contents.get(0).getUserName());
        assertEquals("RED", contents.get(1).getUserName());
        assertEquals(5, total);
    }

    @Test
    void test_complexSearch() {
        final MemberSearchCondition condition = MemberSearchCondition.builder()
                                                                     .ageGoe(20)
                                                                     .ageLoe(40)
                                                                     .build();
        final PageRequest pageable = PageRequest.of(2, 2);
        final Page<MemberTeamDto> page = memberRepository.complexSearch(condition, pageable);
        final List<MemberTeamDto> contents = page.getContent();

        assertEquals(1, contents.size());
    }
}