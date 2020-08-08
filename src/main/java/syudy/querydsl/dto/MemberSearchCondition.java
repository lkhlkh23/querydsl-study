package syudy.querydsl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class MemberSearchCondition {

    private String userName;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

}
