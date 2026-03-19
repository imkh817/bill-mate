package com.billmate.domain.user.repository;

import com.billmate.domain.user.entity.User;
import com.billmate.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("UC-U1: 새 유저를 저장하면 slackUserId로 조회된다")
    void saveAndFindBySlackUserId() {
        User user = TestFixtures.user("U001");
        userRepository.save(user);
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findBySlackUserId("U001");

        assertThat(found).isPresent();
        assertThat(found.get().getSlackUserId()).isEqualTo("U001");
    }

    @Test
    @DisplayName("UC-U2: 동일 slackUserId로 두 번 저장하면 unique 제약 위반 예외")
    void duplicateSlackUserIdThrows() {
        userRepository.save(TestFixtures.user("U_DUP"));
        em.flush();

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(TestFixtures.user("U_DUP"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
