/*******************************************************************************
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.publicissapient.kpidashboard.apis.userboardconfig.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.publicissapient.kpidashboard.apis.abac.UserAuthorizedProjectsService;
import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.auth.service.AuthenticationService;
import com.publicissapient.kpidashboard.apis.common.service.CacheService;
import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.model.application.KpiCategory;
import com.publicissapient.kpidashboard.common.model.application.KpiCategoryMapping;
import com.publicissapient.kpidashboard.common.model.application.KpiMaster;
import com.publicissapient.kpidashboard.common.model.userboardconfig.Board;
import com.publicissapient.kpidashboard.common.model.userboardconfig.BoardDTO;
import com.publicissapient.kpidashboard.common.model.userboardconfig.BoardKpisDTO;
import com.publicissapient.kpidashboard.common.model.userboardconfig.UserBoardConfig;
import com.publicissapient.kpidashboard.common.model.userboardconfig.UserBoardConfigDTO;
import com.publicissapient.kpidashboard.common.repository.application.KpiCategoryMappingRepository;
import com.publicissapient.kpidashboard.common.repository.application.KpiCategoryRepository;
import com.publicissapient.kpidashboard.common.repository.application.KpiMasterRepository;
import com.publicissapient.kpidashboard.common.repository.userboardconfig.UserBoardConfigRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation class for UserBoardConfigService
 * 
 * @author narsingh9
 *
 */
@Service
@Slf4j
public class UserBoardConfigServiceImpl implements UserBoardConfigService {

	private static final String ITERATION = "Iteration";
	private static final String BACKLOG = "Backlog";
	private static final String RELEASE = "Release";
	private static final String DORA = "Dora";
	private static final String KPI_MATURITY = "Kpi Maturity";
	private static final String DEFAULT_BOARD_NAME = "My KnowHow";
	@Autowired
	private UserBoardConfigRepository userBoardConfigRepository;
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private KpiMasterRepository kpiMasterRepository;
	@Autowired
	private KpiCategoryRepository kpiCategoryRepository;
	@Autowired
	private KpiCategoryMappingRepository kpiCategoryMappingRepository;
	@Autowired
	private UserAuthorizedProjectsService authorizedProjectsService;
	@Autowired
	private ConfigHelperService configHelperService;
	@Autowired
	private CacheService cacheService;

	/**
	 * .This method return user board config if present in db else return a default
	 * configuration.
	 *
	 * @return UserBoardConfigDTO
	 */
	public UserBoardConfigDTO getUserBoardConfig() {
		UserBoardConfig existingUserBoardConfig = userBoardConfigRepository
				.findByUsername(authenticationService.getLoggedInUser());
		Iterable<KpiMaster> allKPIs = configHelperService.loadKpiMaster();
		Map<String, KpiMaster> kpiMasterMap = StreamSupport.stream(allKPIs.spliterator(), false)
				.collect(Collectors.toMap(KpiMaster::getKpiId, Function.identity()));
		List<KpiCategory> kpiCategoryList = kpiCategoryRepository.findAll();
		UserBoardConfigDTO defaultUserBoardConfigDTO = new UserBoardConfigDTO();
		if (null == existingUserBoardConfig) {
			setUserBoardConfigBasedOnCategoryForFreshUser(defaultUserBoardConfigDTO, kpiCategoryList, kpiMasterMap);
			return defaultUserBoardConfigDTO;
		} else {
			UserBoardConfigDTO existingUserBoardConfigDTO = convertToUserBoardConfigDTO(existingUserBoardConfig);
			if ((checkKPIAddOrRemoveForExistingUser(existingUserBoardConfigDTO, kpiMasterMap)
					&& checkCategories(existingUserBoardConfigDTO, kpiCategoryList))
					|| checkKPISubCategory(existingUserBoardConfigDTO, kpiMasterMap)) {
				setUserBoardConfigBasedOnCategory(defaultUserBoardConfigDTO, kpiCategoryList, kpiMasterMap);
				filtersBoardsAndSetKpisForExistingUser(existingUserBoardConfigDTO.getScrum(),
						defaultUserBoardConfigDTO.getScrum());
				filtersBoardsAndSetKpisForExistingUser(existingUserBoardConfigDTO.getKanban(),
						defaultUserBoardConfigDTO.getKanban());
				filtersBoardsAndSetKpisForExistingUser(existingUserBoardConfigDTO.getOthers(),
						defaultUserBoardConfigDTO.getOthers());
				return defaultUserBoardConfigDTO;
			}
			filterKpis(existingUserBoardConfigDTO, kpiMasterMap);
			return existingUserBoardConfigDTO;
		}
	}

	/**
	 * This method checks sub tabs for existing user on release board
	 *
	 * @param existingUserBoardConfigDTO
	 *            existingUserBoardConfigDTO
	 * @param kpiMasterMap
	 *            kpiMasterMap
	 * @return kpiMasterMap
	 */
	private boolean checkKPISubCategory(UserBoardConfigDTO existingUserBoardConfigDTO,
			Map<String, KpiMaster> kpiMasterMap) {
		Set<String> existingUserSubCategories = existingUserBoardConfigDTO.getOthers().stream()
				.flatMap(boardDTO -> boardDTO.getKpis().stream().map(BoardKpisDTO::getSubCategoryBoard))
				.filter(Objects::nonNull).collect(Collectors.toSet());
		existingUserSubCategories.addAll(existingUserBoardConfigDTO.getScrum().stream()
				.flatMap(boardDTO -> boardDTO.getKpis().stream().map(BoardKpisDTO::getSubCategoryBoard))
				.filter(Objects::nonNull).collect(Collectors.toSet()));
		existingUserSubCategories.addAll(existingUserBoardConfigDTO.getKanban().stream()
				.flatMap(boardDTO -> boardDTO.getKpis().stream().map(BoardKpisDTO::getSubCategoryBoard))
				.filter(Objects::nonNull).collect(Collectors.toSet()));
		Set<String> kpiMasterSubCategories = kpiMasterMap.values().stream().map(KpiMaster::getKpiSubCategory)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		if (kpiMasterSubCategories.size() > existingUserSubCategories.size()) {
			return !CollectionUtils.containsAll(existingUserSubCategories, kpiMasterSubCategories);
		} else if (kpiMasterSubCategories.size() < existingUserSubCategories.size()) {
			return !CollectionUtils.containsAll(kpiMasterSubCategories, existingUserSubCategories);
		} else {
			return false;
		}
	}

	/**
	 * to check if no new default categories are absent in the existing user board
	 * 
	 * @param existingUserBoardConfigDTO
	 *            existingUserBoardConfigDTO
	 * @param kpiCategoryList
	 *            kpiCategoryList
	 * @return kpiCategoryList
	 */
	private boolean checkCategories(UserBoardConfigDTO existingUserBoardConfigDTO, List<KpiCategory> kpiCategoryList) {
		Set<String> existingCategories = existingUserBoardConfigDTO.getScrum().stream().map(BoardDTO::getBoardName)
				.collect(Collectors.toSet());
		existingCategories.addAll(existingUserBoardConfigDTO.getKanban().stream().map(BoardDTO::getBoardName)
				.collect(Collectors.toSet()));
		existingCategories.addAll(existingUserBoardConfigDTO.getOthers().stream().map(BoardDTO::getBoardName)
				.collect(Collectors.toSet()));

		List<String> defaultKpiCategory = kpiCategoryList.stream().map(KpiCategory::getCategoryName)
				.collect(Collectors.toList());
		defaultKpiCategory.add(ITERATION);
		defaultKpiCategory.add(RELEASE);
		defaultKpiCategory.add(DORA);
		defaultKpiCategory.add(BACKLOG);
		defaultKpiCategory.add(KPI_MATURITY);
		return (!new HashSet<>(defaultKpiCategory).containsAll(existingCategories));
	}

	private void setUserBoardConfigBasedOnCategoryForFreshUser(UserBoardConfigDTO defaultUserBoardConfigDTO,
			List<KpiCategory> kpiCategoryList, Map<String, KpiMaster> kpiMasterMap) {
		setUserBoardConfigBasedOnCategory(defaultUserBoardConfigDTO, kpiCategoryList, kpiMasterMap);

		Optional<UserBoardConfig> findFirstUserBoard = CollectionUtils
				.emptyIfNull(configHelperService.loadUserBoardConfig()).stream().findFirst();
		if (findFirstUserBoard.isPresent()) {
			UserBoardConfig finalBoardConfig = findFirstUserBoard.get();
			List<Board> scrum = finalBoardConfig.getScrum();
			List<Board> kanban = finalBoardConfig.getKanban();
			List<Board> others = finalBoardConfig.getOthers();
			Map<String, Boolean> kpiWiseIsShownFlag = new HashMap<>();
			CollectionUtils.emptyIfNull(scrum).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
					.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));
			CollectionUtils.emptyIfNull(kanban).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
					.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));
			CollectionUtils.emptyIfNull(others).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
					.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));

			CollectionUtils.emptyIfNull(defaultUserBoardConfigDTO.getScrum()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
			CollectionUtils.emptyIfNull(defaultUserBoardConfigDTO.getKanban()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
			CollectionUtils.emptyIfNull(defaultUserBoardConfigDTO.getOthers()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
		}
	}

	/**
	 * check for existing user if any kpi is added or removed from kpi master
	 *
	 * @param existingUserBoardConfig
	 *            existingUserBoardConfig
	 * @param kpiMasterMap
	 *            kpiMasterMap
	 * @return return
	 */
	private boolean checkKPIAddOrRemoveForExistingUser(UserBoardConfigDTO existingUserBoardConfig,
			Map<String, KpiMaster> kpiMasterMap) {
		Set<String> userKpiIdList = new HashSet<>();
		getKpiIdListFromExistingUser(existingUserBoardConfig.getScrum(), userKpiIdList);
		getKpiIdListFromExistingUser(existingUserBoardConfig.getKanban(), userKpiIdList);
		getKpiIdListFromExistingUser(existingUserBoardConfig.getOthers(), userKpiIdList);

		Set<String> kpiMasterKpiIdList = kpiMasterMap.keySet();
		if (kpiMasterKpiIdList.size() > userKpiIdList.size()) {
			return !CollectionUtils.containsAll(userKpiIdList, kpiMasterKpiIdList);
		} else if (kpiMasterKpiIdList.size() < userKpiIdList.size()) {
			return !CollectionUtils.containsAll(kpiMasterKpiIdList, userKpiIdList);
		} else {
			return false;
		}
	}

	/**
	 *
	 * @param existingUserBoardConfig
	 *            existingUserBoardConfig
	 * @param userKpiIdList
	 *            userKpiIdList
	 */
	private void getKpiIdListFromExistingUser(List<BoardDTO> existingUserBoardConfig, Set<String> userKpiIdList) {
		existingUserBoardConfig.forEach(kpiBoard -> {
			kpiBoard.getKpis().removeIf(Objects::isNull);
			Optional.ofNullable(kpiBoard.getKpis()).get().stream().filter(Objects::nonNull)
					.forEach(boardKpisDTO -> userKpiIdList.add(Optional.ofNullable(boardKpisDTO.getKpiId()).get()));
		});
	}

	/**
	 * compare existing user board and default user board (KpiMaster). checked any
	 * new added or remove kpi
	 *
	 * @param existingBoardListDTO
	 *            existingBoardListDTO
	 * @param defaultBoardListDTO
	 *            existingBoardListDTO
	 */
	private void filtersBoardsAndSetKpisForExistingUser(List<BoardDTO> existingBoardListDTO,
			List<BoardDTO> defaultBoardListDTO) {
		defaultBoardListDTO.forEach(defaultBoardDTO -> existingBoardListDTO.forEach(existingBoardDTO -> {
			if (defaultBoardDTO.getBoardId() == existingBoardDTO.getBoardId()
					&& !CollectionUtils.containsAll(defaultBoardDTO.getKpis(), existingBoardDTO.getKpis())) {
				filtersKPIAndSetKPIsForExistingUser(defaultBoardDTO, existingBoardDTO);
			}
			if (defaultBoardDTO.getBoardId() == existingBoardDTO.getBoardId()
					&& !CollectionUtils.containsAll(existingBoardDTO.getKpis(), defaultBoardDTO.getKpis())) {
				filtersKPIAndSetKPIsForExistingUser(defaultBoardDTO, existingBoardDTO);
			}
		}));
	}

	/**
	 * any kpiId added or removed found then we are re place user board config with
	 * default BoardDTO. order maintain only used of user board config when board is
	 * iteration. otherwise work on defaultBoardDTO (kpi_master and
	 * kpi_category_mapping collections)
	 *
	 * @param defaultBoardDTO
	 *            defaultBoardDTO
	 * @param existingBoardDTO
	 *            existingBoardDTO
	 */
	private void filtersKPIAndSetKPIsForExistingUser(BoardDTO defaultBoardDTO, BoardDTO existingBoardDTO) {

		List<BoardKpisDTO> boardKpisList = new ArrayList<>();

		Map<String, BoardKpisDTO> kpiWiseUserBoardConfig = new HashMap<>();
		existingBoardDTO.getKpis()
				.forEach(existingKPI -> kpiWiseUserBoardConfig.put(existingKPI.getKpiId(), existingKPI));
		AtomicInteger iterationOrderSize = new AtomicInteger(2);
		defaultBoardDTO.getKpis().forEach(defaultKPIList -> {
			BoardKpisDTO boardKpis = new BoardKpisDTO();
			boardKpis.setKpiId(defaultKPIList.getKpiId());
			boardKpis.setKpiName(defaultKPIList.getKpiName());
			if (kpiWiseUserBoardConfig.get(defaultKPIList.getKpiId()) != null) {
				BoardKpisDTO existingKPI = kpiWiseUserBoardConfig.get(defaultKPIList.getKpiId());
				boardKpis.setShown(existingKPI.isShown());
				boardKpis.setIsEnabled(existingKPI.getIsEnabled());
				boardKpis.setSubCategoryBoard(existingKPI.getSubCategoryBoard());
				if (defaultBoardDTO.getBoardName().equals(ITERATION)) {
					iterationOrderSize.getAndIncrement();
					boardKpis.setOrder(existingKPI.getOrder());
				} else {
					boardKpis.setOrder(defaultKPIList.getOrder());
				}
			} else {
				boardKpis.setShown(defaultKPIList.isShown());
				boardKpis.setIsEnabled(defaultKPIList.getIsEnabled());
				boardKpis.setSubCategoryBoard(defaultKPIList.getSubCategoryBoard());
				if (!defaultBoardDTO.getBoardName().equalsIgnoreCase(ITERATION)) {
					boardKpis.setOrder(defaultKPIList.getOrder());
				}
			}
			boardKpis.setKpiDetail(defaultKPIList.getKpiDetail());
			boardKpisList.add(boardKpis);
		});
		boardKpisList.stream().filter(boardKpisDTO -> boardKpisDTO.getOrder() == 0)
				.forEach(boardKpisDTO -> boardKpisDTO.setOrder(iterationOrderSize.getAndIncrement()));
		defaultBoardDTO.setKpis(boardKpisList.stream().sorted(Comparator.comparing(BoardKpisDTO::getOrder))
				.collect(Collectors.toList()));
	}

	/**
	 * set user board config for new or default user from kpi master and kpi
	 * category , category mapping.
	 *
	 * @param newUserBoardConfig
	 *            newUserBoardConfig
	 * @param kpiCategoryList
	 *            kpiCategoryList
	 * @param kpiMasterMap
	 *            kpiMasterMap
	 */
	private void setUserBoardConfigBasedOnCategory(UserBoardConfigDTO newUserBoardConfig,
			List<KpiCategory> kpiCategoryList, Map<String, KpiMaster> kpiMasterMap) {
		AtomicReference<Integer> kpiCategoryBoardId = new AtomicReference<>(1);
		newUserBoardConfig.setUsername(authenticationService.getLoggedInUser());
		List<BoardDTO> scrumBoards = new ArrayList<>();
		List<String> defaultKpiCategory = new ArrayList<>();
		defaultKpiCategory.add(ITERATION);
		defaultKpiCategory.add(RELEASE);
		defaultKpiCategory.add(DORA);
		defaultKpiCategory.add(BACKLOG);
		defaultKpiCategory.add(KPI_MATURITY);
		setDefaultBoardInfoFromKpiMaster(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), false,
				defaultKpiCategory, scrumBoards);
		if (CollectionUtils.isNotEmpty(kpiCategoryList)) {
			setAsPerCategoryMappingBoardInfo(kpiCategoryBoardId, kpiCategoryList, kpiMasterMap, scrumBoards, false);
		}
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), ITERATION,
				scrumBoards, false);
		newUserBoardConfig.setScrum(scrumBoards);

		List<BoardDTO> kanbanBoards = new ArrayList<>();
		setDefaultBoardInfoFromKpiMaster(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), true,
				defaultKpiCategory, kanbanBoards);
		if (CollectionUtils.isNotEmpty(kpiCategoryList)) {
			setAsPerCategoryMappingBoardInfo(kpiCategoryBoardId, kpiCategoryList, kpiMasterMap, kanbanBoards, true);
		}
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), ITERATION,
				kanbanBoards, true);
		newUserBoardConfig.setKanban(kanbanBoards);

		List<BoardDTO> otherBoards = new ArrayList<>();
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), RELEASE,
				otherBoards, false);
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), DORA,
				otherBoards, false);
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), BACKLOG,
				otherBoards, false);
		setBoardInfoAsPerDefaultKpiCategory(kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), KPI_MATURITY,
				otherBoards, false);
		newUserBoardConfig.setOthers(otherBoards);
	}

	/**
	 * prepare boards for as per category and kpi category mappings
	 *
	 * @param kpiCategoryBoardId
	 *            kpiCategoryBoardId
	 * @param kpiCategoryList
	 *            kpiCategoryList
	 * @param kpiMasterMap
	 *            kpiMasterMap
	 * @param boardDTOList
	 *            boardDTOList
	 * @param kanban
	 *            kanban
	 */
	private void setAsPerCategoryMappingBoardInfo(AtomicReference<Integer> kpiCategoryBoardId,
			List<KpiCategory> kpiCategoryList, Map<String, KpiMaster> kpiMasterMap, List<BoardDTO> boardDTOList,
			boolean kanban) {
		List<KpiCategoryMapping> kpiCategoryMappingList = kpiCategoryMappingRepository.findAll();
		if (CollectionUtils.isNotEmpty(kpiCategoryMappingList)) {
			Map<String, List<KpiCategoryMapping>> kpiIdWiseCategory = kpiCategoryMappingList.stream()
					.collect(Collectors.groupingBy(KpiCategoryMapping::getCategoryId, Collectors.toList()));
			kpiCategoryList.forEach(kpiCategory -> setBoardInfoAsPerKpiCategory(
					kpiCategoryBoardId.getAndSet(kpiCategoryBoardId.get() + 1), kpiCategory,
					kpiIdWiseCategory.get(kpiCategory.getCategoryId()), kpiMasterMap, boardDTOList, kanban));
		}
	}

	/**
	 * set board details and kpi list as per KPI category.
	 *
	 * @param kpiCategoryBoardId
	 *            kpiCategoryBoardId
	 * @param kpiCategory
	 *            kpiCategory
	 * @param kpiCategoryMappingList
	 *            kpiCategoryMappingList
	 * @param kpiMasterMap
	 *            kpiMasterMap
	 * @param asPerCategoryBoardList
	 *            asPerCategoryBoardList
	 * @param kanban
	 *            kanban
	 */
	private void setBoardInfoAsPerKpiCategory(Integer kpiCategoryBoardId, KpiCategory kpiCategory,
			List<KpiCategoryMapping> kpiCategoryMappingList, Map<String, KpiMaster> kpiMasterMap,
			List<BoardDTO> asPerCategoryBoardList, boolean kanban) {
		BoardDTO asPerCategoryBoard = new BoardDTO();
		asPerCategoryBoard.setBoardId(kpiCategoryBoardId);
		asPerCategoryBoard.setBoardName(kpiCategory.getCategoryName());
		List<BoardKpisDTO> boardKpisList = new ArrayList<>();
		kpiCategoryMappingList.stream().filter(kpiCategoryMapping -> kpiCategoryMapping.isKanban() == kanban)
				.sorted(Comparator.comparing(KpiCategoryMapping::getKpiOrder))
				.forEach(kpiCategoryMapping -> setKpiUserBoardCategoryWise(boardKpisList, kpiCategoryMapping,
						kpiMasterMap.get(kpiCategoryMapping.getKpiId())));
		asPerCategoryBoard.setKpis(boardKpisList);
		asPerCategoryBoardList.add(asPerCategoryBoard);
	}

	/**
	 * set board details and kpi list as per Default category like ITERATION ,
	 * BACKLOG
	 *
	 * @param boardId
	 *            boardId
	 * @param boardName
	 *            boardName
	 * @param asPerCategoryBoardList
	 *            asPerCategoryBoardList
	 * @param kanban
	 *            kanban
	 */
	private void setBoardInfoAsPerDefaultKpiCategory(int boardId, String boardName,
			List<BoardDTO> asPerCategoryBoardList, boolean kanban) {
		BoardDTO asPerCategoryBoard = new BoardDTO();
		asPerCategoryBoard.setBoardId(boardId);
		asPerCategoryBoard.setBoardName(boardName);
		List<BoardKpisDTO> boardKpisList = new ArrayList<>();
		kpiMasterRepository.findByKpiCategoryAndKanban(boardName, kanban).stream()
				.sorted(Comparator.comparing(KpiMaster::getDefaultOrder))
				.forEach(kpiMaster -> setKpiUserBoardDefaultFromKpiMaster(boardKpisList, kpiMaster));
		asPerCategoryBoard.setKpis(boardKpisList);
		asPerCategoryBoardList.add(asPerCategoryBoard);
	}

	/**
	 * set board details and kpi list as per Default category like
	 * DEFAULT_BOARD_NAME.
	 *
	 * @param boardId
	 *            boardId
	 * @param kanban
	 *            kanban
	 * @param kpiCategory
	 *            kpiCategory
	 * @param defaultBoardList
	 *            defaultBoardList
	 */
	private void setDefaultBoardInfoFromKpiMaster(int boardId, boolean kanban, List<String> kpiCategory,
			List<BoardDTO> defaultBoardList) {
		BoardDTO defaultBoard = new BoardDTO();
		defaultBoard.setBoardId(boardId);
		defaultBoard.setBoardName(DEFAULT_BOARD_NAME);
		List<BoardKpisDTO> boardKpisList = new ArrayList<>();
		kpiMasterRepository.findByKanbanAndKpiCategoryNotIn(kanban, kpiCategory).stream()
				.sorted(Comparator.comparing(KpiMaster::getDefaultOrder))
				.forEach(kpiMaster -> setKpiUserBoardDefaultFromKpiMaster(boardKpisList, kpiMaster));
		defaultBoard.setKpis(boardKpisList);
		defaultBoardList.add(defaultBoard);
	}

	/**
	 * set Kpi details in board for user board config from kpi master.
	 *
	 * @param boardKpisList
	 *            boardKpisList
	 * @param kpiMaster
	 *            kpiMaster
	 */
	private void setKpiUserBoardDefaultFromKpiMaster(List<BoardKpisDTO> boardKpisList, KpiMaster kpiMaster) {
		BoardKpisDTO boardKpis = new BoardKpisDTO();
		boardKpis.setKpiId(kpiMaster.getKpiId());
		boardKpis.setKpiName(kpiMaster.getKpiName());
		boardKpis.setShown(true);
		boardKpis.setIsEnabled(true);
		boardKpis.setOrder(kpiMaster.getDefaultOrder());
		boardKpis.setSubCategoryBoard(kpiMaster.getKpiSubCategory());
		boardKpis.setKpiDetail(kpiMaster);
		boardKpisList.add(boardKpis);
	}

	/**
	 * set Kpi details in board for user board config from kpi category mapping
	 *
	 * @param boardKpisList
	 *            boardKpisList
	 * @param kpiCategoryMapping
	 *            kpiCategoryMapping
	 * @param kpiMaster
	 *            kpiMaster
	 */
	private void setKpiUserBoardCategoryWise(List<BoardKpisDTO> boardKpisList, KpiCategoryMapping kpiCategoryMapping,
			KpiMaster kpiMaster) {
		if (Objects.nonNull(kpiMaster)) {
			BoardKpisDTO boardKpis = new BoardKpisDTO();
			boardKpis.setKpiId(kpiCategoryMapping.getKpiId());
			boardKpis.setKpiName(kpiMaster.getKpiName());
			boardKpis.setShown(true);
			boardKpis.setIsEnabled(true);
			boardKpis.setOrder(kpiCategoryMapping.getKpiOrder());
			boardKpis.setKpiDetail(kpiMaster);
			boardKpisList.add(boardKpis);
		} else {
			log.error("[UserBoardConfig]. No kpi Data found for {}", kpiCategoryMapping.getKpiId());

		}
	}

	/**
	 * This method convert user board config to its dto
	 *
	 * @param userBoardConfig
	 *            userBoardConfig
	 * @return UserBoardConfigDTOb
	 */
	private UserBoardConfigDTO convertToUserBoardConfigDTO(UserBoardConfig userBoardConfig) {
		UserBoardConfigDTO userBoardConfigDTO = null;
		if (null != userBoardConfig) {
			ModelMapper mapper = new ModelMapper();
			userBoardConfigDTO = mapper.map(userBoardConfig, UserBoardConfigDTO.class);
		}
		return userBoardConfigDTO;
	}

	/**
	 * added kpi master details in user board config
	 *
	 * @param userBoardConfigDTO
	 *            userBoardConfigDTO
	 * @param kpiDetailMap
	 *            kpiDetailMap
	 */
	private void filterKpis(UserBoardConfigDTO userBoardConfigDTO, Map<String, KpiMaster> kpiDetailMap) {
		if (userBoardConfigDTO != null) {
			addKpiDetails(userBoardConfigDTO.getScrum(), kpiDetailMap);
			addKpiDetails(userBoardConfigDTO.getKanban(), kpiDetailMap);
			addKpiDetails(userBoardConfigDTO.getOthers(), kpiDetailMap);
		}
	}

	private void addKpiDetails(List<BoardDTO> boardList, Map<String, KpiMaster> kpiDetailMap) {
		CollectionUtils.emptyIfNull(boardList).forEach(board -> {
			List<BoardKpisDTO> boardKpiDtoList = new ArrayList<>();
			board.getKpis().forEach(boardKpisDTO -> {
				KpiMaster kpiMaster = kpiDetailMap.get(boardKpisDTO.getKpiId());
				if (null != kpiMaster) {
					boardKpisDTO.setKpiName(kpiMaster.getKpiName());
					boardKpisDTO.setKpiDetail(kpiMaster);
					boardKpiDtoList.add(boardKpisDTO);
				}
			});
			board.setKpis(boardKpiDtoList);
		});
	}

	/**
	 * This method save user board config
	 *
	 * @param userBoardConfigDTO
	 *            userBoardConfigDTO
	 * @return UserBoardConfigDTO
	 */
	@Override
	public UserBoardConfigDTO saveUserBoardConfig(UserBoardConfigDTO userBoardConfigDTO) {
		UserBoardConfig boardConfig = null;
		UserBoardConfig userBoardConfig = convertDTOToUserBoardConfig(userBoardConfigDTO);
		if (userBoardConfig != null && authenticationService.getLoggedInUser().equals(userBoardConfig.getUsername())) {
			boardConfig = userBoardConfigRepository.findByUsername(authenticationService.getLoggedInUser());
			if (null != boardConfig) {
				boardConfig.setScrum(userBoardConfig.getScrum());
				boardConfig.setKanban(userBoardConfig.getKanban());
				boardConfig.setOthers(userBoardConfig.getOthers());
			} else {
				boardConfig = userBoardConfig;
			}
			boardConfig = userBoardConfigRepository.save(boardConfig);

			if (authorizedProjectsService.ifSuperAdminUser()) {
				List<UserBoardConfig> userBoardConfigs = userBoardConfigRepository.findAll();
				updateKpiDetails(userBoardConfigs, boardConfig);
				userBoardConfigRepository.saveAll(userBoardConfigs);
			}
		}
		cacheService.clearCache(CommonConstant.CACHE_USER_BOARD_CONFIG);
		return convertToUserBoardConfigDTO(boardConfig);
	}

	/**
	 * delete user from user_board_config
	 *
	 * @param userName
	 *            userName
	 */
	@Override
	public void deleteUser(String userName) {
		log.info("UserBoardConfigServiceImpl::deleteUser start");
		userBoardConfigRepository.deleteByUsername(userName);
		log.info(userName + " deleted Successfully from user_board_config");
	}

	private void updateKpiDetails(List<UserBoardConfig> userBoardConfigs, UserBoardConfig finalBoardConfig) {
		List<Board> scrum = finalBoardConfig.getScrum();
		List<Board> kanban = finalBoardConfig.getKanban();
		List<Board> others = finalBoardConfig.getOthers();
		Map<String, Boolean> kpiWiseIsShownFlag = new HashMap<>();
		CollectionUtils.emptyIfNull(scrum).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
				.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));
		CollectionUtils.emptyIfNull(kanban).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
				.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));
		CollectionUtils.emptyIfNull(others).stream().flatMap(boardDTO -> boardDTO.getKpis().stream())
				.forEach(boardKpis -> kpiWiseIsShownFlag.put(boardKpis.getKpiId(), boardKpis.isShown()));

		for (UserBoardConfig boardConfig : userBoardConfigs) {
			CollectionUtils.emptyIfNull(boardConfig.getScrum()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
			CollectionUtils.emptyIfNull(boardConfig.getKanban()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
			CollectionUtils.emptyIfNull(boardConfig.getOthers()).stream()
					.flatMap(boardDTO -> boardDTO.getKpis().stream()).forEach(boardKpisDTO -> boardKpisDTO
							.setShown(kpiWiseIsShownFlag.getOrDefault(boardKpisDTO.getKpiId(), true)));
		}
	}

	/**
	 * This method convert userBoardConfigDTO to its userBoardConfig K
	 *
	 * @param userBoardConfigDTO
	 *            userBoardConfigDTO
	 * @return UserBoardConfig
	 */
	private UserBoardConfig convertDTOToUserBoardConfig(UserBoardConfigDTO userBoardConfigDTO) {
		UserBoardConfig userBoardConfig = null;
		if (null != userBoardConfigDTO) {
			ModelMapper mapper = new ModelMapper();
			userBoardConfig = mapper.map(userBoardConfigDTO, UserBoardConfig.class);
		}
		return userBoardConfig;
	}

}
