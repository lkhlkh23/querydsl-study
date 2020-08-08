package syudy.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import syudy.querydsl.dto.MemberSearchCondition;
import syudy.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> simpleSearch(MemberSearchCondition condition, Pageable pageable);

    Page<MemberTeamDto> complexSearch(MemberSearchCondition condition, Pageable pageable);

}
