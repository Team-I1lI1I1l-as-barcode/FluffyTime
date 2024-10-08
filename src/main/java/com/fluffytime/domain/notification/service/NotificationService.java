package com.fluffytime.domain.notification.service;

import com.fluffytime.domain.board.entity.Comment;
import com.fluffytime.domain.board.entity.Mention;
import com.fluffytime.domain.board.entity.Post;
import com.fluffytime.domain.board.entity.Reply;
import com.fluffytime.domain.notification.dto.request.NotificationRequest;
import com.fluffytime.domain.notification.dto.response.NotificationResponse;
import com.fluffytime.domain.notification.entity.Notification;
import com.fluffytime.domain.notification.exception.NotificationNotFound;
import com.fluffytime.domain.notification.repository.NotificationRepository;
import com.fluffytime.domain.user.entity.Profile;
import com.fluffytime.domain.user.entity.User;
import com.fluffytime.domain.user.repository.UserRepository;
import com.fluffytime.global.auth.jwt.util.JwtTokenizer;
import com.fluffytime.global.common.exception.global.UserNotFound;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitters sseEmitters;
    private final JwtTokenizer jwtTokenizer;

    public SseEmitter createSseEmitter(NotificationRequest requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
            .orElseThrow(UserNotFound::new);

        return sseEmitters.createForUser(user.getUserId());
    }

    //댓글 알림 생성
    @Transactional
    public void createCommentsNotification(Post post, User commentAuthor) {
        // 게시글 작성자와 댓글 작성자가 동일한 경우 알림을 생성하지 않음
        if (post.getUser().getUserId().equals(commentAuthor.getUserId())) {
            return; // 알림을 생성하지 않음
        }

        String message = commentAuthor.getNickname() + "님이 회원님의 게시글에 댓글을 달았습니다";
        User user = userRepository.findById(post.getUser().getUserId())
            .orElseThrow(UserNotFound::new);

        String profileImageurl = getProfileImageUrl(commentAuthor.getProfile());

        Notification notification = Notification.builder()
            .message(message)
            .isRead(false)
            .user(user) //알림을 받는 사용자
            .sender(commentAuthor)
            .post(post)
            .type("comment")
            .build();
        notificationRepository.save(notification);

        NotificationResponse responseDto = convertToDto(notification);
        sseEmitters.sendToUser(user.getUserId(), responseDto);
    }

    //답글 알림 생성
    @Transactional
    public void createRepliesNotification(Comment comment, User replyAuthor) {
        // 댓글 작성자와 답글 작성자가 동일한 경우 알림을 생성하지 않음
        if (comment.getUser().getUserId().equals(replyAuthor.getUserId())) {
            return; // 알림을 생성하지 않음
        }

        String message = replyAuthor.getNickname() + "님이 회원님의 댓글에 답글을 달았습니다";
        User user = userRepository.findById(comment.getUser().getUserId())
            .orElseThrow(UserNotFound::new);

        String profileImageurl = getProfileImageUrl(replyAuthor.getProfile());

        Notification notification = Notification.builder()
            .message(message)
            .isRead(false)
            .user(user)
            .sender(replyAuthor)
            .comment(comment)
            .type("reply")
            .build();
        notificationRepository.save(notification);

        NotificationResponse responseDto = convertToDto(notification);
        sseEmitters.sendToUser(user.getUserId(), responseDto);
    }

    //좋아요 알림 생성
    @Transactional
    public void createLikesNotification(Object target, User likeAuthor) {

        String message;
        User targetUser;
        String type;
        Post post = null;
        Comment comment = null;
        Reply reply = null;

        if (target instanceof Post) {
            Post postTarget = (Post) target;
            message = likeAuthor.getNickname() + "님이 회원님의 게시글을 좋아합니다";
            targetUser = postTarget.getUser();

            // 게시글 작성자와 좋아요를 누른 사용자가 동일한 경우 알림 생성하지 않음
            if (targetUser.getUserId().equals(likeAuthor.getUserId())) {
                return;
            }

            type = "postLike";
            post = postTarget;
        } else if (target instanceof Comment) {
            Comment commentTarget = (Comment) target;
            message = likeAuthor.getNickname() + "님이 회원님의 댓글을 좋아합니다";
            targetUser = commentTarget.getUser();

            // 댓글 작성자와 좋아요를 누른 사용자가 동일한 경우 알림 생성하지 않음
            if (targetUser.getUserId().equals(likeAuthor.getUserId())) {
                return;
            }

            type = "commentLike";
            comment = commentTarget;
        } else if (target instanceof Reply) {
            Reply replyTarget = (Reply) target;
            message = likeAuthor.getNickname() + "님이 회원님의 답글을 좋아합니다";
            targetUser = replyTarget.getUser();

            // 답글 작성자와 좋아요를 누른 사용자가 동일한 경우 알림 생성하지 않음
            if (targetUser.getUserId().equals(likeAuthor.getUserId())) {
                return;
            }

            type = "replyLike";
            reply = replyTarget;
        } else {
            throw new IllegalArgumentException("Unsupported target type for like notification");
        }

        Notification notification = Notification.builder()
            .message(message)
            .isRead(false)
            .user(targetUser)
            .sender(likeAuthor)
            .post(post)
            .comment(comment)
            .reply(reply)
            .type(type)
            .build();

        notificationRepository.save(notification);

        NotificationResponse responseDto = convertToDto(notification);
        sseEmitters.sendToUser(targetUser.getUserId(), responseDto);
    }

    //멘션 알림 생성
    @Transactional
    public void createMentionNotification(Mention mention) {
        User mentionedUser = mention.getMetionedUser();
        User targetUser = null;
        String message;
        String type;

        if (mention.getPost() != null) {
            targetUser = mention.getPost().getUser();
            message = targetUser.getNickname() + "님이 회원님을 게시글에 멘션했습니다";
            type = "mentionPost";
        } else if (mention.getComment() != null) {
            targetUser = mention.getComment().getUser();
            message = targetUser.getNickname() + "님이 회원님을 댓글에 멘션했습니다";
            type = "mentionComment";
        } else if (mention.getReply() != null) {
            targetUser = mention.getReply().getUser();
            message = targetUser.getNickname() + "님이 회원님을 답글에 멘션했습니다";
            type = "mentionReply";
        } else {
            throw new IllegalArgumentException("Unsupported mention target");
        }

        // 작성자와 멘션 작성자가 동일한 경우 알림을 생성하지 않음
        if (targetUser.getUserId().equals(mentionedUser.getUserId())) {
            return;
        }

        Notification notification = Notification.builder()
            .message(message)
            .isRead(false)
            .user(mentionedUser)
            .sender(targetUser)
            .post(mention.getPost())
            .comment(mention.getComment())
            .reply(mention.getReply())
            .type(type)
            .build();

        notificationRepository.save(notification);

        NotificationResponse responseDto = convertToDto(notification);
        sseEmitters.sendToUser(targetUser.getUserId(), responseDto);
    }

    // 팔로우 알림 생성
    @Transactional
    public void createFollowNotification(User followingUser, User followedUser) {
        String message = followingUser.getNickname() + "님이 회원님을 팔로우했습니다";

        Notification notification = Notification.builder()
            .message(message)
            .isRead(false)
            .user(followedUser)
            .sender(followingUser)
            .type("follow")
            .build();

        notificationRepository.save(notification);

        NotificationResponse responseDto = convertToDto(notification);
        sseEmitters.sendToUser(followedUser.getUserId(), responseDto);
    }

    //알림 읽음 표시 상태 바꿈
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(NotificationNotFound::new);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    //모든 알림 조회
    @Transactional
    @Cacheable(value = "notifications", key = "#requestDto.userId")
    public List<NotificationResponse> getAllNotifications(NotificationRequest requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
            .orElseThrow(UserNotFound::new);
        List<Notification> notifications = notificationRepository.findByUserOrderByIsReadAscCreatedAtDesc(
            user);

        return notifications.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    //알림 삭제
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(NotificationNotFound::new);

        notificationRepository.delete(notification);
    }

    //accessToken으로 사용자 찾기
    @Transactional(readOnly = true)
    public User findByAccessToken(HttpServletRequest httpServletRequest) {
        String accessToken = jwtTokenizer.getTokenFromCookie(httpServletRequest, "accessToken");

        Long userId = null;
        userId = jwtTokenizer.getUserIdFromToken(accessToken);
        User user = findUserById(userId).orElseThrow(UserNotFound::new);
        return user;
    }

    //사용자 조회
    @Transactional
    public Optional<User> findUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user;
    }

    @Transactional
    public NotificationResponse convertToDto(Notification notification) {
        Long postId = null;
        Long commentId = null;
        Long replyId = null;

        User sender = notification.getSender(); // sender 가져오기
        String profileImageurl =
            sender != null ? getProfileImageUrl(sender.getProfile()) : "/image/profile/default.png";
        String senderNickname = sender != null ? sender.getNickname() : "Unknown";

        // 알림 타입이 팔로우일 경우, 관련 ID는 모두 null로 설정
        if (!notification.getType().equals("follow")) {
            if (notification.getReply() != null) {
                // Reply를 통해 Post의 ID 가져오기
                postId = notification.getReply().getComment().getPost().getPostId();
                replyId = notification.getReply().getReplyId();
            } else if (notification.getComment() != null) {
                // Comment를 통해 Post의 ID 가져오기
                postId = notification.getComment().getPost().getPostId();
                commentId = notification.getComment().getCommentId();
            } else if (notification.getPost() != null) {
                // 직접 Post가 있을 때의 처리
                postId = notification.getPost().getPostId();
            }
        }

        return NotificationResponse.builder()
            .notificationId(notification.getNotificationId())
            .message(notification.getMessage())
            .isRead(notification.isRead())
            .type(notification.getType())
            .userId(notification.getUser().getUserId())
            .nickname(senderNickname)
            .postId(postId) // postId 설정
            .commentId(commentId)
            .replyId(replyId)
            .createdAt(notification.getCreatedAt())
            .profileImageurl(profileImageurl)
            .build();
    }


    //프로필 이미지 response
    private String getProfileImageUrl(Profile profile) {
        return Optional.ofNullable(profile)
            .flatMap(p -> Optional.ofNullable(p.getProfileImages()))
            .map(profileImages -> profileImages.getFilePath())
            .orElse("/image/profile/profile.png");
    }

}
