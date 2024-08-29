//일단 정적 페이지에 대한 팔로우만.. ex)userpage, mypage

// 버튼 클릭 후 api 호출 함수
async function toggleFollow(button, action, targetUserNickname) {
  console.log("팔로우/언팔로우 api 호출");
  const url = action === "add" ? "/api/follow/add" : "/api/follow/remove";

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        followedUserNickname: targetUserNickname
      })
    });

    if (!response.ok) {
      throw new Error('팔로우/언팔로우 api 호출 결과에서 오류 발생!');
    }

    await checkFollowStatus(button, targetUserNickname); // 상태 갱신
    await updateFollowCounts(targetUserNickname); // 팔로우 수 갱신
  } catch (error) {
    console.error('toggleFollow() Error:', error);
  }
}

// 팔로우 상태 확인 함수
async function checkFollowStatus(button, targetUserNickname) {
  console.log("팔로우 상태 확인 api 호출");
  try {
    const response = await fetch(
        `/api/follow/status?nickname=${encodeURIComponent(targetUserNickname)}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json"
          }
        });

    if (!response.ok) {
      throw new Error('팔로우 상태 확인 중 오류 발생!');
    }

    const isFollowing = await response.json();
    updateButtonStatus(button, isFollowing);
  } catch (error) {
    console.error('checkFollowStatus() Error:', error);
  }
}

//버튼 상태 갱신 함수
function updateButtonStatus(button, isFollowing) {
  console.log("버튼 상태 갱신 api 호출");
  if (isFollowing) {
    button.textContent = "팔로잉";
    button.dataset.action = "remove";
    button.style.backgroundColor = "#CCCCCC";
  } else {
    button.textContent = "팔로우";
    button.dataset.action = "add";
    button.style.backgroundColor = "#FFCC00";
  }
}

//팔로워/팔로우 수 갱신 함수
async function updateFollowCounts(nickname) {
  console.log("팔로워/팔로우 카운트 갱신 api 호출");
  try {
    const response = await fetch(`/api/follow/count/${nickname}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }
    });

    if (!response.ok) {
      throw new Error('팔로워/팔로잉 수 조회 중 오류 발생');
    }

    const data = await response.json();
    document.getElementById("follower_count").textContent = data.followerCount;
    document.getElementById("follow_count").textContent = data.followingCount;
  } catch (error) {
    console.error('updateFollowCounts() Error:', error);
  }
}

// 팔로우 버튼 클릭 이벤트 핸들러
async function handleFollowButtonClick(event) {
  const button = event.target.closest(".follow_button");
  if (!button) {
    return;
  } // 클릭된 요소가 팔로우 버튼이 아니면 무시

  // 경로를 '/'로 분리하여 배열로 만들고 마지막 요소인 유저 닉네임을 얻는다
  const path = window.location.pathname;
  const pathSegments = path.split('/');
  const targetUserNickname = pathSegments[pathSegments.length - 1];

  const action = button.dataset.action || "add"; // 초기 액션은 "add"

  await checkFollowStatus(button, targetUserNickname);
  await toggleFollow(button, action, targetUserNickname);
}

//모달

// 모달 열기 함수
function openModal(modal) {
  modal.style.display = "block";
}

// 모달 닫기 함수
function closeModal(modal) {
  modal.style.display = "none";
}

// 모달 외부 클릭 시 닫기 함수
window.onclick = function (event) {
  const followerModal = document.getElementById('followerList-modal');
  const followingModal = document.getElementById('followingList-modal');

  // 팔로워 모달이 열려 있고, 클릭한 대상이 팔로워 모달인 경우 모달을 닫기
  if (followerModal && event.target === followerModal) {
    closeModal(followerModal);
  }

  // 팔로잉 모달이 열려 있고, 클릭한 대상이 팔로잉 모달인 경우 모달을 닫기
  if (followingModal && event.target === followingModal) {
    closeModal(followingModal);
  }
}

// 모달 닫기 버튼 클릭 시 닫기 함수
document.querySelectorAll('.follow-close').forEach(closeButton => {
  closeButton.onclick = function () {
    const followerModal = document.getElementById('followerList-modal');
    const followingModal = document.getElementById('followingList-modal');

    if (followerModal && followerModal.style.display === 'block') {
      closeModal(followerModal);
    } else if (followingModal && followingModal.style.display === 'block') {
      closeModal(followingModal);
    }
  };
});
//
// // 팔로워 리스트 API 호출 함수
// async function fetchFollowerList(nickname) {
//   console.log("팔로워 리스트 api 호출");
//   try {
//     const response = await fetch(`/api/follow/search/followers/${nickname}`, {
//       method: "GET",
//       headers: {
//         "Content-Type": "application/json"
//       }
//     });
//
//     if (!response.ok) {
//       throw new Error('팔로워 목록을 가져오는 중 오류 발생');
//     }
//
//     const followers = await response.json();
//     const followerListElement = document.getElementById('followerList');
//     followerListElement.innerHTML = ''; // 기존 목록 초기화
//
//     followers.forEach(follower => {
//       const li = document.createElement('li');
//       li.className = 'follower-item'; // 클래스 이름 추가
//
//       const linkBox = document.createElement('div');
//       linkBox.className = 'linkBox';
//
//       const img = document.createElement('img');
//       img.src = follower.profileImageUrl; // 프로필 이미지 URL 설정
//       img.alt = `${follower.nickname}의 프로필 이미지`; // 이미지 대체 텍스트 설정
//       img.className = 'follower-profile-image'; // 프로필 이미지에 대한 클래스 이름 설정
//
//       const details = document.createElement('div');
//       details.className = 'details';
//
//       const nickname = document.createElement('div');
//       nickname.className = 'nickname';
//       nickname.textContent = follower.nickname; // 팔로워 닉네임 표시
//
//       const intro = document.createElement('div');
//       intro.className = 'intro';
//       intro.textContent = follower.intro; // 팔로워의 한줄 소개 표시
//
//       // "자기 소개 없음"이면 글씨 회색으로
//       if (intro.textContent === "자기 소개 없음") {
//         intro.style.color = '#A9A9A9';
//       }
//
//       details.appendChild(nickname);
//       details.appendChild(intro);
//
//       linkBox.appendChild(img);
//       linkBox.appendChild(details);
//
//       //클릭 시 유저 페이지로 이동
//       linkBox.addEventListener('click', () => {
//         window.location.href = `/userpages/${follower.nickname}`;
//       });
//
//       li.appendChild(linkBox);
//
//       // 리스트 항목을 팔로워 목록에 추가
//       followerListElement.appendChild(li);
//     });
//
//     const modal = document.getElementById('followerList-modal');
//     openModal(modal);
//   } catch (error) {
//     console.error('fetchFollowerList() Error:', error);
//   }
// }
//
// // 팔로잉 리스트 API 호출 함수
// async function fetchFollowingList(nickname) {
//   console.log("팔로잉 리스트 api 호출");
//   try {
//     const response = await fetch(`/api/follow/search/followings/${nickname}`, {
//       method: "GET",
//       headers: {
//         "Content-Type": "application/json"
//       }
//     });
//
//     if (!response.ok) {
//       throw new Error('팔로잉 목록을 가져오는 중 오류 발생');
//     }
//
//     const followings = await response.json();
//     const followingListElement = document.getElementById('followingList');
//     followingListElement.innerHTML = ''; // 기존 목록 초기화
//
//     followings.forEach(following => {
//       const li = document.createElement('li');
//       li.className = 'following-item'; // 클래스 이름 추가
//
//       const linkBox = document.createElement('div');
//       linkBox.className = 'linkBox';
//
//       const img = document.createElement('img');
//       img.src = following.profileImageUrl; // 프로필 이미지 URL 설정
//       img.alt = `${following.nickname}의 프로필 이미지`; // 이미지 대체 텍스트 설정
//       img.className = 'following-profile-image'; // 프로필 이미지에 대한 클래스 이름 설정
//
//       const details = document.createElement('div');
//       details.className = 'details';
//
//       const nickname = document.createElement('div');
//       nickname.className = 'nickname';
//       nickname.textContent = following.nickname; // 팔로워 닉네임 표시
//
//       const intro = document.createElement('div');
//       intro.className = 'intro';
//       intro.textContent = following.intro; // 팔로워의 한줄 소개 표시
//
//       // "자기 소개 없음"이면 글씨 회색으로
//       if (intro.textContent === "자기 소개 없음") {
//         intro.style.color = '#A9A9A9';
//       }
//
//       details.appendChild(nickname);
//       details.appendChild(intro);
//
//       linkBox.appendChild(img);
//       linkBox.appendChild(details);
//
//       //클릭 시 유저 페이지로 이동
//       linkBox.addEventListener('click', () => {
//         window.location.href = `/userpages/${following.nickname}`;
//       });
//
//       li.appendChild(linkBox);
//
//       // 리스트 항목을 팔로워 목록에 추가
//       followingListElement.appendChild(li);
//     });
//
//     const modal = document.getElementById('followingList-modal');
//     openModal(modal);
//   } catch (error) {
//     console.error('fetchFollowingList() Error:', error);
//   }
// }
async function fetchList(type, nickname) {
  console.log(`${type} 리스트 API 호출`);
  try {
    const response = await fetch(`/api/follow/search/${type}s/${nickname}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }
    });

    if (!response.ok) {
      throw new Error(`${type} 목록을 가져오는 중 오류 발생`);
    }

    const items = await response.json();
    const listElement = document.getElementById(`${type}List`);
    listElement.innerHTML = ''; // 기존 목록 초기화

    items.forEach(item => {
      const li = document.createElement('li');
      li.className = `${type}-item`; // 클래스 이름 추가

      const linkBox = document.createElement('div');
      linkBox.className = 'linkBox';

      const img = document.createElement('img');
      img.src = item.profileImageUrl; // 프로필 이미지 URL 설정
      img.alt = `${item.nickname}의 프로필 이미지`; // 이미지 대체 텍스트 설정
      img.className = `${type}-profile-image`; // 프로필 이미지에 대한 클래스 이름 설정

      const details = document.createElement('div');
      details.className = 'details';

      const nicknameDiv = document.createElement('div');
      nicknameDiv.className = 'nickname';
      nicknameDiv.textContent = item.nickname; // 팔로워/팔로잉 닉네임 표시

      const intro = document.createElement('div');
      intro.className = 'intro';
      intro.textContent = item.intro; // 팔로워/팔로잉의 한줄 소개 표시

      // "자기 소개 없음"이면 글씨 회색으로
      if (intro.textContent === "자기 소개 없음") {
        intro.style.color = '#A9A9A9';
      }

      details.appendChild(nicknameDiv);
      details.appendChild(intro);

      linkBox.appendChild(img);
      linkBox.appendChild(details);

      // 클릭 시 유저 페이지로 이동
      linkBox.addEventListener('click', () => {
        window.location.href = `/userpages/${item.nickname}`;
      });

      li.appendChild(linkBox);

      // 리스트 항목을 리스트에 추가
      listElement.appendChild(li);
    });

    const modal = document.getElementById(`${type}List-modal`);
    openModal(modal);
  } catch (error) {
    console.error(
        `fetch ${type}List() Error:`, error);
  }
}

// 팔로워 리스트 API 호출 함수
async function fetchFollowerList(nickname) {
  await fetchList('follower', nickname);
}

// 팔로잉 리스트 API 호출 함수
async function fetchFollowingList(nickname) {
  await fetchList('following', nickname);
}

// 페이지 로드 시 이벤트 핸들러 설정
document.addEventListener("DOMContentLoaded", async function () {
  // 경로를 '/'로 분리하여 배열로 만들고 마지막 요소인 유저 닉네임을 얻는다
  const path = window.location.pathname;
  const pathSegments = path.split('/');// 동적 생성 팔로우는 더 고려해봐야 함..
  const targetUserNickname = pathSegments[pathSegments.length - 1];

  //  팔로우 버튼 팔로우 상태를 확인하고 상태를 화면에 적용
  const followButton = document.querySelector(".follow_button");
  if (followButton) {
    await checkFollowStatus(followButton, targetUserNickname);
  }
  //팔로우,팔로잉 유저 수를 업데이트
  await updateFollowCounts(targetUserNickname);

  // 정적으로 생성된 팔로우 버튼에 대한 이벤트 핸들러
  document.body.addEventListener("click", handleFollowButtonClick);

  // 팔로워 목록 불러오기 클릭 이벤트
  const followerText = document.querySelector("#follower_list_modal");
  if (followerText) {
    followerText.addEventListener("click", async function () {
      await fetchFollowerList(targetUserNickname);
    });
  }

  // 팔로잉 목록 불러오기 클릭 이벤트
  const followingText = document.querySelector("#following_list_modal");
  if (followingText) {
    followingText.addEventListener("click", async function () {
      await fetchFollowingList(targetUserNickname);
    });
  }

});