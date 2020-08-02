package syudy.querydsl.entity;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
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

    private JPAQueryFactory query;

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

        this.query = new JPAQueryFactory(em);
    }

    @Test
    @DisplayName("Member 객체 삽입 후 조회")
    void test_select_01() {
       final List<Member> members = em.createQuery("select m from Member m", Member.class)
                                      .getResultList();

       assertEquals(5, members.size());
    }

    @Test
    @DisplayName("Member 객체 조회 where 조건문")
    void test_select_02_where_01() {
        final QMember member = QMember.member;
        final Member findMember = query.select(member).from(member)
                                       .where(member.username.like("%DO%").and(member.age.gt(20)))
                                       .fetchOne();

        /*
            gt  >
            goe >=
            lt  <
            loe <=
        */

        assertNotNull(findMember);
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("Member 객체 조회 where 조건문")
    void test_select_02_where_02() {
        final QMember member = QMember.member;
        /* 같은 and 조건에 대해서는 , (comma) 활용 가능 */
        final Member findMember = query.select(member).from(member)
                .where(member.username.like("%DO%"), (member.age.gt(20)))
                .fetchOne();

        assertNotNull(findMember);
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("Member 객체 조회 fetch")
    void test_select_03_fetch_01() {
        final QMember member = QMember.member;
        /*
            fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
            fetchOne() : 단 건 조회
                결과가 없으면 : null,
                결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException

            fetchFirst() : limit(1).fetchOne()
            fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
            fetchCount() : count 쿼리로 변경해서 count 수 조회
        */

        final List<Member> fetch = query.selectFrom(member).fetch();
        assertEquals(5, fetch.size());

        final Member fetchOne = query.selectFrom(member)
                                     .where(member.username.eq("BRAD"))
                                     .fetchOne();
        assertNull(fetchOne);

        final Member fetchFirst = query.select(member).from(member).fetchFirst();
        assertNotNull(fetchFirst);

        /* PAGING에서 사용하는 조회 방식, totalCount + select 두개의 쿼리가 실제로 동작 */
        final QueryResults<Member> results = query.selectFrom(member).fetchResults();
        assertEquals(5, results.getTotal());
        assertEquals(5, results.getResults().size());

        final long fetchCount = query.selectFrom(member).fetchCount();
        assertEquals(5, fetchCount);
    }

    @Test
    @DisplayName("Member 객체 조회 fetch, fetchOne를 시도했을 경우, 데이터가 1개 초과이면 예외발생")
    void test_select_03_fetch_02() {
        final QMember member = QMember.member;

        assertThrows(NonUniqueResultException.class, () ->{
            query.selectFrom(member).fetchOne();
        });
    }

    @Test
    @DisplayName("Member 객체 조회 정렬")
    void test_select_04_sort_01() {
        final QMember member = QMember.member;

        /* 나이 많은 순서, 이름 가나다 순서이고, 이름이 NULL이면 마지막 */
        final List<Member> members = query.selectFrom(member)
                                          .orderBy(member.age.desc(), member.username.asc().nullsLast())
                                          .fetch();
        assertEquals("RED", members.get(0).getUsername());
        assertEquals("LIME", members.get(3).getUsername());
        assertNull(members.get(4).getUsername());
    }

    @Test
    @DisplayName("Member 객체 조회 페이")
    void test_select_05_page_01() {
        final QMember member = QMember.member;
        final QueryResults<Member> memberResults = query.selectFrom(member)
                                                        .orderBy(member.age.desc().nullsFirst())
                                                        .offset(2)
                                                        .limit(2)
                                                        .fetchResults();
        final List<Member> members = memberResults.getResults();
        assertEquals(5, memberResults.getTotal());
        assertEquals(2, members.size());
        assertEquals("NORI", members.get(0).getUsername());
        assertNull(members.get(1).getUsername());
    }

    @Test
    @DisplayName("Member 객체 조회 집합")
    void test_select_06_aggregation_01() {
        final QMember member = QMember.member;
        final Tuple tuple = query.select(
                                    member.count(),
                                    member.age.sum(),
                                    member.age.avg(),
                                    member.age.max(),
                                    member.age.min())
                                .from(member)
                                .fetchOne();

        assertEquals(5, tuple.get(member.count()));
        assertEquals(28, tuple.get(member.age.min()));
        assertEquals(33, tuple.get(member.age.max()));
    }

    @Test
    @DisplayName("Member 객체 조회 Group by, 팀의 이름과 각 팀의 평균 연령 구하기")
    void test_select_06_aggregation_02() {
        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        final List<Tuple> tuples = query.select(team.name, member.age.avg())
                                        .from(team)
                                        .join(member)
                                        .on(team.eq(member.team))
                                        .groupBy(team.name)
                                        .fetch();

        assertEquals(32, tuples.get(0).get(member.age.avg()));
    }
}