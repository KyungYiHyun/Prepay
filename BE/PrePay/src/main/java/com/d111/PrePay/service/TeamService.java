package com.d111.PrePay.service;

import com.d111.PrePay.value.RequestStatus;
import com.d111.PrePay.dto.request.*;
import com.d111.PrePay.dto.respond.GetUserOfTeamRes;
import com.d111.PrePay.dto.respond.StoresRes;
import com.d111.PrePay.dto.respond.TeamDetailRes;
import com.d111.PrePay.dto.respond.TeamRes;
import com.d111.PrePay.dto.request.TeamCreateStoreReq;
import com.d111.PrePay.dto.request.TeamCreateReq;
import com.d111.PrePay.dto.respond.*;
import com.d111.PrePay.model.Team;
import com.d111.PrePay.model.TeamStore;
import com.d111.PrePay.model.User;
import com.d111.PrePay.model.UserTeam;
import com.d111.PrePay.repository.TeamRepository;
import com.d111.PrePay.repository.TeamStoreRepository;
import com.d111.PrePay.repository.UserRepository;
import com.d111.PrePay.repository.UserTeamRepository;
import com.d111.PrePay.model.*;
import com.d111.PrePay.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;
    private final TeamStoreRepository teamStoreRepository;
    private final StoreRepository storeRepository;
    private final ChargeRequestRepository chargeRequestRepository;
    private final PartyRequestRepository partyRequestRepository;
    private final ImageService imageService;
    private final StoreService storeService;


    // 좋아요 한 퍼블릭 팀
    public List<PublicTeamLikedRes> showPublicLiked(Long userId, float latitude, float longitude) {
        List<UserTeam> userTeams = userTeamRepository.findUserTeamsByUserId(userId);
        List<PublicTeamLikedRes> result = new ArrayList<>();
        for (UserTeam userTeam : userTeams) {
            if (userTeam.getTeam().isPublicTeam() && userTeam.isLike()) {
                PublicTeamLikedRes res = new PublicTeamLikedRes(userTeam, userTeam.getTeam());
                Store store = userTeam.getTeam().getTeamStores().get(0).getStore();
                float dis = storeService.calDistance(store.getLongitude(), store.getLatitude(), longitude, latitude);
                res.setDistance(dis);
                result.add(res);
            }
        }
        return result;
    }


    // 팀 이미지 업로드
    @Transactional
    public UploadImageRes uploadImage(TeamIdReq req, MultipartFile image) {
        Team team = teamRepository.findById(req.getTeamId()).orElseThrow();
        if (image != null && !image.isEmpty()) {
            String imgUrl;
            try {
                imgUrl = imageService.uploadImage(image);
                imageService.uploadImage(image);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            team.setTeamImgUrl(imgUrl);
        }

        UploadImageRes uploadImageRes = UploadImageRes.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .imgUrl(team.getTeamImgUrl())
                .build();

        return uploadImageRes;
    }


    // 팀 사용자 추방
    public void banUser(BanUserReq req) {

        UserTeam findUserTeam = userTeamRepository.findByTeam_IdAndUser_Email(req.getTeamId(), req.getBanUserEmail())
                .orElseThrow();
        userTeamRepository.delete(findUserTeam);
    }


    // 팀 나가기
    @Transactional
    public void exitTeam(Long userId, TeamIdReq req) {
        UserTeam findUserTeam = userTeamRepository.findByTeamIdAndUserId(req.getTeamId(), userId)
                .orElseThrow();
        if (findUserTeam.isPosition()) {
            userTeamRepository.deleteByTeam_Id(req.getTeamId());
        } else {
            userTeamRepository.delete(findUserTeam);
        }

    }


    // 팀 회식 권한 요청 처리
    @Transactional
    public PartyConfirmRes confirmPrivilege(PartyConfirmReq req) {
        PartyRequest findPartyRequest = partyRequestRepository.findById(req.getPartyRequestId())
                .orElseThrow();
        if (req.isAccept()) {
            findPartyRequest.setRequestStatus(RequestStatus.Approved);
        } else {
            findPartyRequest.setRequestStatus(RequestStatus.Refused);
        }

        PartyConfirmRes partyConfirmRes = PartyConfirmRes.builder()
                .partyRequestId(findPartyRequest.getId())
                .requestStatus(findPartyRequest.getRequestStatus())
                .build();
        return partyConfirmRes;
    }


    // 팀 회식 권한 요청
    public PartyRequestRes privilegeRequest(Long userId, TeamIdReq req) {
        UserTeam findUserTeam = userTeamRepository.findByTeamIdAndUserId(req.getTeamId(), userId)
                .orElseThrow();
        PartyRequest savePartyRequest = partyRequestRepository.save(PartyRequest.builder()
                .requestDate(System.currentTimeMillis())
                .requestStatus(RequestStatus.Waiting)
                .statusChangedDate(0)
                .userTeam(findUserTeam)
                .build());

        return PartyRequestRes.builder()
                .partyRequestId(savePartyRequest.getId())
                .requestDate(savePartyRequest.getRequestDate())
                .requestStatus(savePartyRequest.getRequestStatus())
                .statusChangedDate(savePartyRequest.getStatusChangedDate())
                .build();
    }


    // 팀 가맹점 잔액 충전 요청
    public ChargeRes chargeRequest(ChargeReq req) {
        TeamStore findTeamStore = teamStoreRepository.findByTeamIdAndStoreId(req.getTeamId(), req.getStoreId())
                .orElseThrow();
        ChargeRequest saveChargeRequest = chargeRequestRepository.save(ChargeRequest.builder()
                .requestStatus(RequestStatus.Waiting)
                .requestPrice(req.getRequestPrice())
                .requestDate(System.currentTimeMillis())
                .teamStore(findTeamStore)
                .build());

        return ChargeRes.builder()
                .chargeRequestId(saveChargeRequest.getId())
                .RequestStatus(saveChargeRequest.getRequestStatus())
                .requestPrice(saveChargeRequest.getRequestPrice())
                .requestDate(saveChargeRequest.getRequestDate())
                .build();
    }


    // 팀 비밀번호를 이용한 팀 가입
    // 확인
    @Transactional
    public GetUserOfTeamRes signInTeam(Long userId, SignInTeamReq req) {
        Team findTeam = teamRepository.findByTeamPassword(req.getTeamPassword())
                .orElseThrow(() -> new NoSuchElementException("일치하는 팀이 없습니다."));

        User findUser = userRepository.findById(userId).orElseThrow();

        if (userTeamRepository.existsByUserAndTeam(findUser, findTeam)) {
            throw new DuplicateKeyException("이미 가입된 팀입니다.");
        }

        if (findTeam.getCodeGenDate() < System.currentTimeMillis() - 1000 * 60 * 60 * 3) {
            throw new NoSuchElementException("초대 코드 시간 만료");
        }
        UserTeam userTeam = UserTeam.builder()
                .team(findTeam)
                .user(findUser)
                .privilege(false)
                .usageCount(0)
                .usedAmount(0)
                .position(false)
                .isLike(false)
                .build();
        userTeamRepository.save(userTeam);
        GetUserOfTeamRes getUserOfTeamRes = new GetUserOfTeamRes(userTeam);
        return getUserOfTeamRes;
    }


    // 팀 회식 권한 부여
    @Transactional
    public GrantPrivilegeRes grantPrivilege(GrantPrivilegeReq req) {
        UserTeam findUserTeam = userTeamRepository.findByTeamIdAndUser_Email(req.getTeamId(), req.getChangeUserEmail())
                .orElseThrow();
        findUserTeam.setPrivilege(req.isPrivilege());

        GrantPrivilegeRes grantPrivilegeRes = GrantPrivilegeRes.builder()
                .changeUserEmail(req.getChangeUserEmail())
                .teamId(req.getTeamId())
                .privilege(findUserTeam.isPrivilege())
                .build();

        return grantPrivilegeRes;
    }


    // 팀 운영자 권한 부여
    // 확인
    @Transactional
    public GrantAdminPositionRes grantAdminPosition(GrantAdminPositionReq req) {
        UserTeam findUserTeam = userTeamRepository.findByTeamIdAndUserId(req.getTeamId(), req.getChangeUserId())
                .orElseThrow();
        findUserTeam.setPosition(req.isPosition());
        GrantAdminPositionRes grantAdminPositionRes = GrantAdminPositionRes.builder()
                .changeUserId(req.getChangeUserId())
                .teamId(req.getTeamId())
                .position(findUserTeam.isPosition())
                .build();

        return grantAdminPositionRes;
    }


    // 팀 한도 변경
    // 확인
    @Transactional
    public TeamDetailRes changeDailyPriceLimit(ChangeDailyPriceLimitReq req, Long userId) {
        UserTeam userTeam = userTeamRepository.findUserTeamByTeamIdAndUserIdWithTeam(req.getTeamId(), userId);
        Team findTeam = userTeam.getTeam();
        findTeam.setDailyPriceLimit(req.getDailyPriceLimit());

        TeamDetailRes teamDetailRes = new TeamDetailRes(findTeam, userTeam);
        return teamDetailRes;
    }


    // 팀 초대 코드 생성
    // 확인
    @Transactional
    public InviteCodeRes generateInviteCode(Long userId, TeamIdReq req) {
        Team team = teamRepository.findById(req.getTeamId()).orElseThrow();
        String password = generateRandomPassword();
        team.setTeamPassword(password);
        team.setCodeGenDate(System.currentTimeMillis());
        return new InviteCodeRes(password, System.currentTimeMillis());
    }


    // 팀 가맹점 추가
    // 확인
    @Transactional
    public TeamCreateStoreRes createStore(TeamCreateStoreReq req) {
        log.info("팀아이디 : {}, 스토어아이디 : {}", req.getTeamId(), req.getStoreId());
        Team findTeam = teamRepository.findById(req.getTeamId()).orElseThrow(() -> new NoSuchElementException("팀 없음"));
        Store findStore = storeRepository.findById(req.getStoreId()).orElseThrow(() -> new NoSuchElementException("스토어 없음"));


        TeamStore teamStore = new TeamStore(findTeam, findStore);
        TeamStore savedTeamStore = teamStoreRepository.save(teamStore);

        TeamCreateStoreRes teamCreateStoreRes = TeamCreateStoreRes.builder()
                .teamStoreId(savedTeamStore.getId())
                .teamId(findTeam.getId())
                .storeId(findStore.getId())
                .teamStoreBalance(savedTeamStore.getTeamStoreBalance())
                .storeImgUrl(findStore.getStoreImgUrl())
                .build();

        return teamCreateStoreRes;
    }


    // 팀 유저 조회
    // 완료
    // 팀 찾을때 유저팀 찾기
    // 유저팀 찾을 때 유저 찾기
    public List<GetUserOfTeamRes> getUsersOfTeam(Long teamId, Long userId) {
        Team team = teamRepository.findTeamWithUserTeamAndUserByTeamId(teamId);
        List<GetUserOfTeamRes> resultList = new ArrayList<>();
        GetUserOfTeamRes ownerUser = null;
        int idx = 0;
        for (UserTeam userTeam : team.getUserTeams()) {
            if (userTeam.isPosition()) {
                ownerUser = new GetUserOfTeamRes(userTeam);
                resultList.add(idx, ownerUser);
                idx++;
            } else {
                GetUserOfTeamRes getUserOfTeamRes = new GetUserOfTeamRes(userTeam);
                resultList.add(getUserOfTeamRes);
            }
        }

        return resultList;
    }


    // 팀 상세 조회
    // 지연로딩 설정
    // 완료
    public TeamDetailRes getTeamDetails(Long teamId, Long userId) {
//        UserTeam findUserTeam = userTeamRepository.findByTeamIdAndUserId(teamId, userId)
//                .orElseThrow(() -> new RuntimeException("유저팀을 찾을 수 없습니다."));
        UserTeam findUserTeam = userTeamRepository.findUserTeamByTeamIdAndUserIdWithTeam(teamId, userId);
        Team findTeam = findUserTeam.getTeam();
        List<TeamStore> teamStores = findTeam.getTeamStores();
        int sum = 0;
        for (TeamStore teamStore : teamStores) {
            sum += teamStore.getTeamStoreBalance();
        }
        TeamDetailRes res = TeamDetailRes.builder()
                .teamId(teamId)
                .teamName(findTeam.getTeamName())
                .dailyPriceLimit(findTeam.getDailyPriceLimit())
                .publicTeam(findTeam.isPublicTeam())
                .countLimit(findTeam.getCountLimit())
                .teamMessage(findTeam.getTeamMessage())
                .position(findUserTeam.isPosition())
                .teamPassword(findTeam.getTeamPassword())
                .usedAmount(findUserTeam.getUsedAmount())
                .teamBalance(sum)
                .color(findTeam.getColor())
                .build();

        return res;
    }


    // 팀 생성
    // 완료
    public TeamCreateRes createTeam(TeamCreateReq request, Long userId, MultipartFile image) {
        String teamPassword;
        String teamColor;
        if (!request.isPublicTeam()) {
            teamPassword = generateRandomPassword();
            teamColor = request.getColor();
        } else {
            teamPassword = null;
            teamColor = null;
        }

        Team team = Team.builder()
                .teamName(request.getTeamName())
                .publicTeam(request.isPublicTeam())
                .teamPassword(teamPassword)
                .dailyPriceLimit(request.getDailyPriceLimit())
                .countLimit(request.getCountLimit())
                .teamMessage(request.getTeamMessage())
                .color(teamColor)
                .genDate(System.currentTimeMillis())
                .teamInitializer(userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("유저를 찾을 수 없습니다.")))
                .build();


        Team savedTeam = teamRepository.save(team);

        if (image != null && !image.isEmpty()) {
            String imgUrl = null;
            try {
                imgUrl = imageService.uploadImage(image);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            team.setTeamImgUrl(imgUrl);
            teamRepository.save(team);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("유저를 찾을 수 없습니다."));

        UserTeam userTeam = UserTeam.builder()
                .team(savedTeam)
                .user(user)
                .position(true)
                .privilege(true)
                .usedAmount(0)
                .usageCount(0)
                .isLike(false)
                .build();

        userTeamRepository.save(userTeam);
        TeamCreateRes teamCreateRes = TeamCreateRes.builder()
                .teamId(team.getId())
                .build();
        return teamCreateRes;
    }

    //나의 팀 전체 조회
    //확인 -> 완료
    public List<TeamRes> getMyTeams(long userId) {
        // 유저 가져올때 유저팀 같이 가져오기
        // 유저팀 가져올때 팀 같이 가져오기

        User user = userRepository.findUserById(userId);
        List<UserTeam> userTeams = userTeamRepository.findUserTeamsByUserId(userId);
        List<TeamRes> resultList = new ArrayList<>();
        for (UserTeam userTeam : userTeams) {
            if (userTeam.getTeam().isPublicTeam()) continue;
            int sumTeamBalance = 0;
            TeamRes teamRes = new TeamRes(userTeam, sumTeamBalance);
            List<TeamStore> teamStores = userTeam.getTeam().getTeamStores();
            for (TeamStore teamStore : teamStores) {
                sumTeamBalance += teamStore.getTeamStoreBalance();
            }
            teamRes.setTeamBalance(sumTeamBalance);
            resultList.add(teamRes);
        }

        return resultList;

    }


    //팀의 가게 조회
    public List<StoresRes> getMyTeamStores(long teamId, long userId) {
        // 팀 찾을 때 팀스토어 같이 찾기
        // 팀스토어 찾을 때 스토어 같이 찾기
//        List<TeamStore> teamStores = teamStoreRepository.findByTeamId(teamId);
//        return teamStores.stream().map((teamStore)->{
//            StoresRes storesRes = new StoresRes(teamStore);
//            storesRes.setLatitude(teamStore.getStore().getLatitude());
//            storesRes.setLongitude(teamStore.getStore().getLongitude());
//            storesRes.setMyteam(true);
//            return storesRes;
//        }).collect(Collectors.toList());
        Team team = teamStoreRepository.findTeamWithTeamStoreAndStoreByTeamId(teamId);
        return team.getTeamStores().stream()
                .map(teamStore -> {
                    StoresRes storesRes = new StoresRes(teamStore);
                    storesRes.setLatitude(teamStore.getStore().getLatitude());
                    storesRes.setLongitude(teamStore.getStore().getLongitude());
                    storesRes.setMyteam(true);
                    storesRes.setImgUrl(teamStore.getStore().getStoreImgUrl());
                    return storesRes;
                }).collect(Collectors.toList());
    }

//        Team team = teamRepository.findById(teamId).orElseThrow();
//        List<TeamStore> teamStores = team.getTeamStores();
//        List<StoresRes> resultList = new ArrayList<>();
//        for (TeamStore teamStore : teamStores) {
//            StoresRes storesRes = new StoresRes(teamStore);
//            storesRes.setLatitude(teamStore.getStore().getLatitude());
//            storesRes.setLongitude(teamStore.getStore().getLongitude());
//            resultList.add(storesRes);
//        }
//        return resultList;
//    }


    //팀 가맹점의 좌표 조회
    //완료
    public List<StoresCorRes> getStoresCor(long teamId, long userId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        List<TeamStore> list = teamStoreRepository.findTeamStoresWithStoreByTeam(team);
        List<StoresCorRes> result = new ArrayList<>();
        for (TeamStore teamStore : list) {
            log.info("팀 스토어 : {}", teamStore);
            Store store = teamStore.getStore();
            StoresCorRes storesCorRes = new StoresCorRes(store);
            result.add(storesCorRes);
        }
        return result;
    }

    //퍼블릭 팀 리스트 조회
    // 완료
    public List<PublicTeamsRes> getPublicTeams(String email, float latitude, float longitude) {
        List<Team> teams = teamRepository.findTeamsWithUserByPublicTeam(true);
        List<PublicTeamsRes> resultList = new ArrayList<>();
        for (Team team : teams) {
            PublicTeamsRes publicTeamsRes = new PublicTeamsRes(team);
            publicTeamsRes.setTeamInitializerNickname(team.getTeamInitializer().getNickname());
            Optional<UserTeam> userTeam = userTeamRepository.findByTeamIdAndUser_Email(team.getId(), email);
            if (userTeam.isPresent()) {
                publicTeamsRes.setLike(userTeam.get().isLike());
            } else {
                publicTeamsRes.setLike(false);
            }
            Store store = team.getTeamStores().get(0).getStore();
            float distance = storeService.calDistance(store.getLongitude(), store.getLatitude(), longitude, latitude);
            publicTeamsRes.setDistance(distance);
            publicTeamsRes.setAddress(store.getAddress());
            resultList.add(publicTeamsRes);
        }
        return resultList;
    }

    //퍼브릭 팀 검색 리스트
    //완료
    public List<PublicTeamsRes> getPublicTeamsByKeyword(String keyword) {
//        List<Team> teams = teamRepository.findTeamsByTeamNameContaining(keyword);
        List<Team> teams = teamRepository.findTeamsbyKeywordNoN(keyword);
        List<PublicTeamsRes> resultList = new ArrayList<>();
        for (Team team : teams) {
            PublicTeamsRes publicTeamsRes = new PublicTeamsRes(team);
            publicTeamsRes.setTeamInitializerNickname(team.getTeamInitializer().getNickname());
            resultList.add(publicTeamsRes);
        }
        return resultList;
    }

    // 랜덤 비밀번호 생성
    public String generateRandomPassword() {
        String password = UUID.randomUUID().toString();
        return password;
    }


    public InviteCodeRes getTeamInviteCode(String email, long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        String teamPassword = team.getTeamPassword();
        if (teamPassword == null || System.currentTimeMillis() - (1000 * 60 * 60 * 3) > team.getCodeGenDate()) {
            throw new NoSuchElementException("코드 시간 만료 재발급 문의");
        }

        return new InviteCodeRes(teamPassword, team.getCodeGenDate());
    }

    @Transactional
    public StandardRes like(String email, LikeReq req) {
        Optional<UserTeam> opUserTeam = userTeamRepository.findByTeamIdAndUser_Email(req.getTeamId(), email);
        UserTeam userTeam;
        log.info("좋아요 : {}", req.isCheckLike());
        if (opUserTeam.isEmpty()) {
            User user = userRepository.findUserByEmail(email);
            Team team = teamRepository.findById(req.getTeamId()).orElseThrow();
            userTeam = UserTeam.builder()
                    .team(team)
                    .user(user)
                    .privilege(false)
                    .usageCount(0)
                    .usedAmount(0)
                    .position(false)
                    .isLike(req.isCheckLike())
                    .build();
            userTeamRepository.save(userTeam);
        } else {
            userTeam = opUserTeam.get();
            userTeam.setLike(req.isCheckLike());
        }
        return new StandardRes("좋아요 완료", 200);
    }

    @Transactional
    public PublicTeamDetailRes getPublicTeamDetail(String email, long teamId) {
        Optional<UserTeam> opUserTeam = userTeamRepository.findByTeamIdAndUser_Email(teamId, email);
        UserTeam userTeam;
        if (opUserTeam.isEmpty()) {
            User user = userRepository.findUserByEmail(email);
            Team team = teamRepository.findById(teamId).orElseThrow(() -> new NoSuchElementException("팀 없음"));
            userTeam = UserTeam.builder()
                    .team(team)
                    .user(user)
                    .privilege(false)
                    .usageCount(0)
                    .usedAmount(0)
                    .position(false)
                    .isLike(false)
                    .build();
            userTeamRepository.save(userTeam);
        } else {
            userTeam = opUserTeam.get();
        }
        return new PublicTeamDetailRes(userTeam, userTeam.getTeam());
    }

    public List<PublicTeams2kmRes> get2kmPublicTeams(String email, float latitude, float longitude) {
        List<Store> stores = storeRepository.findAll();
        List<Store> in2Km = new ArrayList<>();
        HashMap<Long, Double> disMap = new HashMap<>();
        HashMap<Long, String> addMap = new HashMap<>();
        for (Store store : stores) {
            double dis = storeService.calDistance(store.getLongitude(), store.getLatitude(), longitude, latitude);
            if (storeService.calDistance(store.getLongitude(), store.getLatitude(), longitude, latitude) < 2L) {
                in2Km.add(store);
                addMap.put(store.getId(), store.getAddress());
                disMap.put(store.getId(), dis);
            }
        }

        List<PublicTeams2kmRes> result = new ArrayList<>();
        for (Store store : in2Km) {
            List<TeamStore> teamStores = store.getTeamStores();
            for (TeamStore teamStore : teamStores) {
                Team team = teamStore.getTeam();
                boolean check = false;
                for (PublicTeams2kmRes tmpResult : result) {
                    if (tmpResult.getTeamId() == team.getId()) {
                        check = true;
                        break;
                    }
                }
                if (check) continue;

                if (team.isPublicTeam()) {
                    Optional<UserTeam> opUserTeam = userTeamRepository.findByTeamIdAndUser_Email(team.getId(), email);
                    PublicTeams2kmRes res = new PublicTeams2kmRes(team);
                    res.setLatitude(store.getLatitude());
                    res.setLongitude(store.getLongitude());
                    res.setTeamInitializerNickname(team.getTeamInitializer().getNickname());
                    if (opUserTeam.isPresent()) {
                        res.setLike(opUserTeam.get().isLike());
                    }
                    res.setAddress(addMap.get(store.getId()));
                    res.setDistance(disMap.get(store.getId()));
                    result.add(res);
                }
            }
        }
        log.info("리스트 사이즈 : {}", result.size());
        return result;
    }

    public PrivateStoreDetail getPrivateDetail(String email, long teamId, long storeId) {
        TeamStore teamStore = teamStoreRepository.findByTeamIdAndStoreId(teamId, storeId).orElseThrow(()->new NoSuchElementException("teamStore 없음"));
        Store store = teamStore.getStore();
        return new PrivateStoreDetail(teamStore,store);
    }
}