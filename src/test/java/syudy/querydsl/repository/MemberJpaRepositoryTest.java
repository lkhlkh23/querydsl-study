package syudy.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class MemberJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        final Member member = new Member("DOBY", 31);
        memberJpaRepository.save(member);
    }

    @Test
    void test_save() {
        final Member member = new Member("NORI", 30);
        memberJpaRepository.save(member);

        final Member saved = memberJpaRepository.findByUserName("NORI");
        assertEquals("NORI", saved.getUsername());
        assertEquals(30, saved.getAge());
    }

    @Test
    void test_findById() {
        final Member saved = memberJpaRepository.findByUserName("DOBY");
        assertEquals("DOBY", saved.getUsername());
        assertEquals(31, saved.getAge());
    }

    @Test
    void test_findAll() {
        final Member member = new Member("NORI", 30);
        memberJpaRepository.save(member);

        final List<Member> members = memberJpaRepository.findAll();
        assertEquals(2, members.size());
    }

    @Test
    void test_findByUserName() {
        final Member saved = memberJpaRepository.findByUserName("DOBY");
        assertEquals("DOBY", saved.getUsername());
        assertEquals(31, saved.getAge());
    }

    @Test
    void test_findAllUsingQuerydsl() {
        final Member member = new Member("NORI", 30);
        memberJpaRepository.save(member);

        final List<Member> members = memberJpaRepository.findAllUsingQuerydsl();
        assertEquals(2, members.size());
    }

    @Test
    void test_findByUserNameUsingQuerydsl() {
        final Member saved = memberJpaRepository.findByUserNameUsingQuerydsl("DOBY");
        assertEquals("DOBY", saved.getUsername());
        assertEquals(31, saved.getAge());
    }

    @Test
    void test_searchByBuilder() {
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

        final MemberSearchCondition condition = MemberSearchCondition.builder()
                                                                     .ageGoe(31)
                                                                     .ageLoe(32)
                                                                     .build();
        final List<MemberTeamDto> dto = memberJpaRepository.searchByBuilder(condition);
        assertEquals("DOBY", dto.get(0).getUserName());
    }

    @Test
    void test_search() {
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

        final MemberSearchCondition condition = MemberSearchCondition.builder()
                                                                     .ageGoe(31)
                                                                     .ageLoe(32)
                                                                     .build();
        final List<MemberTeamDto> dto = memberJpaRepository.search(condition);
        assertEquals("DOBY", dto.get(0).getUserName());
    }

}