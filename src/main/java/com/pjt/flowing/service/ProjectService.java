package com.pjt.flowing.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.pjt.flowing.dto.AuthorizationDto;
import com.pjt.flowing.dto.request.invite.KickMemberRequestDto;
import com.pjt.flowing.dto.request.project.ProjectCreateRequestDto;
import com.pjt.flowing.dto.request.invite.AcceptRequestDto;
import com.pjt.flowing.dto.response.*;
import com.pjt.flowing.dto.request.project.ProjectEditRequestDto;
import com.pjt.flowing.dto.response.document.DocumentIdResponseDto;
import com.pjt.flowing.dto.response.gapnode.GapTableIdResponseDto;
import com.pjt.flowing.dto.response.invite.CheckingNameByEmailResponseDto;
import com.pjt.flowing.dto.response.node.NodeTableIdResponseDto;
import com.pjt.flowing.dto.response.project.ProjectMemberResponseDto;
import com.pjt.flowing.dto.response.project.ProjectResponseDto;
import com.pjt.flowing.dto.response.project.ProjectTestResponseDto;
import com.pjt.flowing.exception.BadRequestException;
import com.pjt.flowing.exception.ErrorCode;
import com.pjt.flowing.model.*;
import com.pjt.flowing.model.document.Document;
import com.pjt.flowing.model.folder.Folder;
import com.pjt.flowing.model.folder.FolderTable;
import com.pjt.flowing.model.gapnode.GapTable;
import com.pjt.flowing.model.node.NodeTable;
import com.pjt.flowing.model.project.Bookmark;
import com.pjt.flowing.model.project.Project;
import com.pjt.flowing.model.project.ProjectMember;
import com.pjt.flowing.model.swot.SWOT;
import com.pjt.flowing.repository.*;
import com.pjt.flowing.repository.document.DocumentRepository;
import com.pjt.flowing.repository.folder.FolderRepository;
import com.pjt.flowing.repository.folder.FolderTableRepository;
import com.pjt.flowing.repository.gapnode.GapTableRepository;
import com.pjt.flowing.repository.node.NodeTableRepository;
import com.pjt.flowing.repository.project.BookmarkRepository;
import com.pjt.flowing.repository.project.ProjectMemberRepository;
import com.pjt.flowing.repository.project.ProjectRepository;
import com.pjt.flowing.repository.swot.SWOTRepository;
import com.pjt.flowing.security.Authorization;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final DocumentRepository documentRepository;
    private final NodeTableRepository nodeTableRepository;
    private final GapTableRepository gapTableRepository;
    private final FolderTableRepository folderTableRepository;
    private final FolderRepository folderRepository;
    private final SWOTRepository swotRepository;
    private final Authorization authorization;
    private final NodeService nodeService;
    private final DocumentService documentService;
    private final GapNodeService gapNodeService;
    private final SWOTService swotService;

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public List<ProjectResponseDto> getAll(Long userId) {//휴지통 제외하고 보내주기
        //여기서 폴더에있는건 빼고 보내줘야함...
        List<Long> folderProjectList=new ArrayList<>();
        List<FolderTable> folderTableList = folderTableRepository.findAllByMember_Id(userId);
        for(FolderTable folderTable : folderTableList){
            List<Folder> folderList = folderRepository.findAllByFolderTable_Id(folderTable.getId());
            for(Folder folder : folderList){
                folderProjectList.add(folder.getProjectId());
            }
        }
       List<ProjectMember> myIncludedProjects = projectMemberRepository.findAllByMember_Id(userId); // 자기가 포함된 프로젝트 리스트
       List<ProjectResponseDto> includedDto = myIncludedProjects.stream()
               .filter(x -> !x.getProject().isTrash())
               .filter(x-> !folderProjectList.contains(x.getProject().getId())) // 폴더안에 프로젝트가 없다면 뽑아라
               .map(ProjectResponseDto::includedProject)
               .sorted(Comparator.comparing(ProjectResponseDto::getModifiedAt).reversed())
               .collect(Collectors.toList());
        return includedDto;
    }
    // getAll 테스트
    public List<ProjectTestResponseDto> getAll2(Long userId) {
        //여기서 폴더에있는건 빼고 보내줘야함...
        List<Long> folderProjectList=new ArrayList<>();
        List<FolderTable> folderTableList = folderTableRepository.findAllByMember_Id(userId);
        for(FolderTable folderTable : folderTableList){
            List<Folder> folderList = folderRepository.findAllByFolderTable_Id(folderTable.getId());
            for(Folder folder : folderList){
                folderProjectList.add(folder.getProjectId());
            }
        }
        List<ProjectMember> myIncludedProjects = projectMemberRepository.findAllByMember_Id(userId);
        List<ProjectTestResponseDto> includeDto = new ArrayList<>();
        for (ProjectMember projectMember : myIncludedProjects) {
            List<String> nicknames = new ArrayList<>();
            projectMember.getProject().getProjectMemberList().stream()
                    .map(c -> c.getMember().getNickname())
                    .forEach(s-> nicknames.add(s));
            boolean bookmarkCheck = bookmarkRepository.existsByMember_IdAndProject_Id(projectMember.getMember().getId(), projectMember.getProject().getId());
            ProjectTestResponseDto responseDto = new ProjectTestResponseDto(
                projectMember.getProject().getId(),
                projectMember.getProject().getProjectName(),
                projectMember.getProject().getModifiedAt(),
                nicknames,
                projectMember.getProject().getThumbNailNum(),
                projectMember.getProject().isTrash(),
                bookmarkCheck
            );
            includeDto.add(responseDto);
        }
        return includeDto.stream()
                .filter(x-> !x.isTrash())
                .filter(x-> !folderProjectList.contains(x.getProjectId())) // 폴더안에 프로젝트가 없다면 뽑아라
                .sorted(Comparator.comparing(ProjectTestResponseDto::getModifiedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public String deleteProject(Long projectId, AuthorizationDto dto) {
        JSONObject obj = new JSONObject();
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("func/ deleteProject/ project Id")
        );
        if (dto.getUserId() == project.getMember().getId()) {
            projectRepository.deleteById(projectId);
            obj.put("msg", "삭제완료");
        }
        else {
            obj.put("msg", "프로젝트 장이 아닙니다");
        }
        return obj.toString();
    }

    @Transactional
    public String editProject(Long projectId, ProjectEditRequestDto dto) {
        JSONObject obj = new JSONObject();
        Optional<Project> project = projectRepository.findById(projectId);
        if (Objects.equals(dto.getUserId(), project.get().getMember().getId())) {
            project.get().update(dto);
            obj.put("msg", "수정 완료");
        }
        else {
            obj.put("msg", "프로젝트 장이 아닙니다");
        }
        return obj.toString();
    }

    public String detail(Long projectId) {
        JSONObject obj = new JSONObject();
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("projectId error")
        );
        //한개만있음
        Document document = documentRepository.findByProject_Id(projectId);
        GapTable gapTable = gapTableRepository.findByProject_Id(projectId);
        NodeTable nodeTable = nodeTableRepository.findByProject_Id(projectId);
        SWOT swot = swotRepository.findByProject_Id(projectId);
        ProjectResponseDto dto = ProjectResponseDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .modifiedAt(project.getModifiedAt())
                .thumbnailNum(project.getThumbNailNum())
                .build();
        List<ProjectMember> projectMemberList = projectMemberRepository.findAllByProject_Id(projectId);
        List<ProjectMemberResponseDto> projectMemberResponseDtoList = new ArrayList<>();
        for (ProjectMember projectMember : projectMemberList) {
            ProjectMemberResponseDto projectMemberResponseDto = new ProjectMemberResponseDto(projectMember.getMember().getId(),
                    projectMember.getMember().getNickname(),projectMember.getMember().getProfileImageURL());
            projectMemberResponseDtoList.add(projectMemberResponseDto);
        }
        obj.put("msg", "불러오기");
        JSONObject DTO = new JSONObject(dto);
        obj.put("projectInfo", DTO);
        obj.put("documentId", document.getId());
        obj.put("gapTableId", gapTable.getId());
        obj.put("nodeTable", nodeTable.getId());
        obj.put("swotId",swot.getId());
        obj.put("projectMemberInfoList",projectMemberResponseDtoList);
        return obj.toString();
    }

    public List<ProjectResponseDto> getAllBookmarked(Long userId) {
        List<Bookmark> bookmarked = bookmarkRepository.findAllByMember_IdOrderByModifiedAtDesc(userId); //userId가 누른 북마크
        return bookmarked.stream()
                .map(ProjectResponseDto::from2)
                .collect(Collectors.toList());
    }

    public List<ProjectResponseDto> getAllCreate(Long userId) {
        List<Project> myCreateProjects = projectRepository.findAllByMember_IdAndTrashOrderByModifiedAtDesc(userId, false); // 자기가 만든 프로젝트 리스트
        return myCreateProjects.stream()
                .map(ProjectResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public String accept(AcceptRequestDto acceptRequestDto) {
        Long projectId = acceptRequestDto.getProjectId();
        Long userId = acceptRequestDto.getUserId();
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("accept (project) error")
        );
        Member member = memberRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("accept (member) error")
        );
        ProjectMember projectMember = new ProjectMember(project, member);
        projectMemberRepository.save(projectMember);
        JSONObject obj = new JSONObject();
        obj.put("msg", "수락 완료");
        return obj.toString();
    }

    public String showTemplates(Long projectid) {
        List<Document> documentList = documentRepository.findAllByProject_Id(projectid);
        List<NodeTable> nodeTableList = nodeTableRepository.findAllByProject_Id(projectid);
        List<GapTable> gapTableList = gapTableRepository.findAllByProject_Id(projectid);
        List<SWOT> swotList = swotRepository.findAllByProject_Id(projectid);
        List<DocumentIdResponseDto> documentIdResponseDtoList = new ArrayList<>();
        for (Document document : documentList) {
            DocumentIdResponseDto documentIdResponseDto = new DocumentIdResponseDto(document.getId());
            documentIdResponseDtoList.add(documentIdResponseDto);
        }
        List<NodeTableIdResponseDto> nodeTableIdResponseDtoList = new ArrayList<>();
        for (NodeTable nodeTable : nodeTableList) {
            NodeTableIdResponseDto nodeTableIdResponseDto = new NodeTableIdResponseDto(nodeTable.getId());
            nodeTableIdResponseDtoList.add(nodeTableIdResponseDto);
        }
        List<GapTableIdResponseDto> gapTableIdResponseDtoList = new ArrayList<>();
        for (GapTable gapTable : gapTableList) {
            GapTableIdResponseDto gapTableIdResponseDto = new GapTableIdResponseDto(gapTable.getId());
            gapTableIdResponseDtoList.add(gapTableIdResponseDto);
        }
        List<SwotIdResponseDto> swotIdResponseDtoList = new ArrayList<>();
        for (SWOT swot : swotList) {
            SwotIdResponseDto swotIdResponseDto = new SwotIdResponseDto(swot.getId());
            swotIdResponseDtoList.add(swotIdResponseDto);
        }
        JSONObject obj = new JSONObject();
        obj.put("msg", "템플릿 리스트 불러오기");
        obj.put("documentIdList", documentIdResponseDtoList);
        obj.put("nodeTableIdList", nodeTableIdResponseDtoList);
        obj.put("gapTableIdList", gapTableIdResponseDtoList);
        obj.put("swotIdList", swotIdResponseDtoList);
        return obj.toString();
    }

    //프로젝트 생성하기
    @Transactional
    public String createProject(ProjectCreateRequestDto projectCreateRequestDto) throws JsonProcessingException {
        AuthorizationDto authorizationDto = new AuthorizationDto(projectCreateRequestDto.getAccessToken(), projectCreateRequestDto.getKakaoId(), projectCreateRequestDto.getUserId());
        JSONObject obj = new JSONObject();
        if (authorization.getKakaoId(authorizationDto) == 0) {
            obj.put("msg", "false");
            return obj.toString();
        }
        Member member = memberRepository.findById(projectCreateRequestDto.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("func/ createProject/ member Id")
        );
        Project project = new Project(
                projectCreateRequestDto.getProjectName(),
                projectCreateRequestDto.getObjectId(),
                member,
                projectCreateRequestDto.getThumbNailNum()
        );
        projectRepository.save(project);
        ProjectMember projectMember = new ProjectMember(project, member);
        projectMemberRepository.save(projectMember);
        nodeService.nodeTableCreate(project.getId());
        gapNodeService.gapTableCreate(project.getId());
        documentService.documentCreate(project.getId());
        swotService.swotCreate(project.getId());
        Document document = documentRepository.findByProject_Id(project.getId());
        GapTable gapTable = gapTableRepository.findByProject_Id(project.getId());
        NodeTable nodeTable = nodeTableRepository.findByProject_Id(project.getId());
        SWOT swot = swotRepository.findByProject_Id(project.getId());
        ProjectResponseDto dto = ProjectResponseDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .modifiedAt(project.getModifiedAt())
                .thumbnailNum(project.getThumbNailNum())
                .build();
        obj.put("msg", "생성하고 불러오기");
        JSONObject DTO = new JSONObject(dto);
        obj.put("projectInfo", DTO);
        obj.put("documentId", document.getId());
        obj.put("gapTableId", gapTable.getId());
        obj.put("nodeTable", nodeTable.getId());
        obj.put("swotId",swot.getId());
        return obj.toString();
    }

    //북마크 생성하기
    @Transactional
    public String checkBookmark(Long projectId, AuthorizationDto authorizationDto) {
        boolean check = bookmarkRepository.existsByMember_IdAndProject_Id(authorizationDto.getUserId(), projectId);
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("func/ checkBookmark/ Project Id")
        );
        Member member = memberRepository.findById(authorizationDto.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("func/ checkBookmark/ member Id")
        );
        JSONObject obj = new JSONObject();
        if (!check) {
            Bookmark bookmark = new Bookmark(project, member);
            bookmarkRepository.save(bookmark);
            obj.put("msg", "check");
        }
        else {
            bookmarkRepository.deleteByMember_IdAndProject_Id(authorizationDto.getUserId(), projectId);
            obj.put("msg", "cancel");
        }
        return obj.toString();
    }

    //프로젝트 검색
    public List<ProjectResponseDto> searchAll(Long userId,String text) {//휴지통 제외하고 보내주기
        List<ProjectMember> myIncludedProjects = projectMemberRepository.findAllByMember_Id(userId); // 자기가 포함된 프로젝트 리스트
        List<ProjectResponseDto> includedSearchDto = myIncludedProjects.stream()
                .filter(x -> !x.getProject().isTrash())                         // 쓰레기통에 안간거 걸러주고
                .filter(x -> x.getProject().getProjectName().contains(text))    // 검색한 단어가 프로젝트명에 들어가있는것들 뽑아주고
                .map(ProjectResponseDto::includedProject)
                .sorted(Comparator.comparing(ProjectResponseDto::getModifiedAt).reversed()) //modifiedAt으로 최신순으로 만들어주고
                .collect(Collectors.toList());
        return includedSearchDto;
    }


    // 프로젝트에서 멤버 추방하기
    @Transactional
    public String kickMember(KickMemberRequestDto requestDto) {
        JSONObject obj = new JSONObject();
        if (!projectRepository.existsByMember_IdAndId(requestDto.getUserId(), requestDto.getProjectId())){
            obj.put("msg", "추방할 권리가 없습니다.");
            return obj.toString();
        }
        projectMemberRepository.deleteByMember_IdAndProject_Id(requestDto.getMemberId(), requestDto.getProjectId());
        obj.put("msg", "추방 완료");
        return obj.toString();
    }
    // 멤버초대하는 메세지에 닉네임과 이미지 넘겨주기
    @Transactional
    public String checkingNameByEmail(String email) {
        if (!memberRepository.existsByEmail(email)) {
            throw new BadRequestException(ErrorCode.USER_EMAIL_NOT_FOUND);
        }
        Member member = memberRepository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("func/ checkingNameByEmail/ not exist email")
        );
        CheckingNameByEmailResponseDto responseDto = new CheckingNameByEmailResponseDto(
                member.getNickname(),
                member.getProfileImageURL()
        );
        JSONObject obj = new JSONObject(responseDto);
        return obj.toString();
    }
}
