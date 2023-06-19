/*******************************************************************************
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

import { Component, OnInit, ElementRef, ViewChild,HostListener, OnDestroy } from '@angular/core';
import { HttpService } from '../../services/http.service';
import { SharedService } from '../../services/shared.service';
import { HelperService } from '../../services/helper.service';
import { GoogleAnalyticsService } from '../../services/google-analytics.service';
import { GetAuthorizationService } from 'src/app/services/get-authorization.service';
import { Router } from '@angular/router';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MessageService, MenuItem } from 'primeng/api';
import { faRotateRight } from '@fortawesome/fontawesome-free';
import { NgSelectComponent } from '@ng-select/ng-select';
import { NotificationResponseDTO } from 'src/app/model/NotificationDTO.model';
import { first } from 'rxjs/operators';

@Component({
  selector: 'app-filter',
  templateUrl: './filter.component.html',
  styleUrls: ['./filter.component.css'],
})
export class FilterComponent implements OnInit, OnDestroy {
  @ViewChild('selector') ngselect: NgSelectComponent;
  @ViewChild('toggleButton') toggleButton: ElementRef;
  @ViewChild('drpmenu') drpmenu: ElementRef;
  @ViewChild('dateToggleButton') dateToggleButton: ElementRef;
  @ViewChild('dateDrpmenu') dateDrpmenu: ElementRef;

  isSuperAdmin = false;
  masterData: any = {};
  filterData: any = [];
  shareDataObject: any = {};
  selectedFilterCount = 0;
  getData: any = [];
  filterRequestData = {};
  filterkeys: any = [];
  selectedFilterData: any = {};
  selectedTab;
  disableDownloadBtn = false;
  subscriptions: any[] = [];
  filterKpiRequest: any = '';
  kanban = false;
  filterType = 'Default';
  maxDate = new Date(); // setting max date user can select in calendar
  showIndicator = false;
  toggleDropdown = false;
  kpiListData: any = {};
  kpiList = [];
  showKpisList = [];
  kpiForm: UntypedFormGroup;
  activeSprintList: any = [];
  currentSelectedSprintId: any;
  noAccessMsg = false;
  filterForm: UntypedFormGroup;
  toggleFilterDropdown = false;
  selectedFilterArray: Array<any> = [];
  faRotateRight: faRotateRight;
  filterApplyData = {};
  colorObj = {};
  tempParentArray: Array<any> = [];
  selectedNodes = {};
  selectedNodeLevel = 0;
  hierarchyLevels = [];
  trendLineValueList: any = [];
  toggleDateDropdown = false;
  filteredSprints = [];
  showDropdown = {};
  selectedDateFilter = '';
  beginningDate;
  selectedProjectLastSyncDate: any;
  selectedProjectLastSyncDetails: any;
  selectedProjectLastSyncStatus: any;
  processorsTracelogs = [];
  processorName = ['jira' , 'azure'];
  heirarchyCount: number;
  dateRangeFilter: any;
  selectedDayType = 'Weeks';
  selectedDays: any;
  previousType = false; // to check if Scrum/Kanban selection has changed
  takeFiltersFromPreviousTab: boolean; // to check if previous tab was following the same filter format
  additionalFiltersArr = [];
  additionalFiltersDdn = {};
  toggleDropdownObj = {};
  hierarchies;
  filteredAddFilters = {};
  initFlag = true;
  showChart = true;
  iterationConfigData = {};
  kpisNewOrder = [];
  isTooltip = false;
  projectIndex = 0;
  notificationList = [];
  items: MenuItem[]  = [
    {
      label: 'Help',
      icon: 'fa fa-info-circle',
      command: () => {
        this.service.setSideNav(false);
        this.router.navigate(['/dashboard/Help']);
      },
    },
    {
      label: 'Logout',
      icon: 'fas fa-sign-out-alt',
      command: () => {
        this.logout();
      },
    },
  ];
  username: string;
  isGuest = false;
  logoImage: any;
  totalRequestCount = 0;
  selectedProjectData ={};
  allowMultipleSelection = true;
  defaultFilterSelection = true;
  selectedSprint={};
  noProjects = false;
  selectedRelease ={};

  constructor(
    private service: SharedService,
    private httpService: HttpService,
    private getAuthorizationService: GetAuthorizationService,
    public router: Router,
    private ga: GoogleAnalyticsService,
    private messageService: MessageService,
    private helperService: HelperService
  ) { }

  ngOnInit() {
    this.service.currentUserDetailsObs.subscribe(details=>{
      if(details){
        this.username = details['user_name'];
      }
    });

    this.selectedTab = this.service.getSelectedTab() || 'mydashboard';
    this.service.setSelectedDateFilter(this.selectedDayType);
    this.service.setShowTableView(this.showChart);
    this.getNotification();
    this.initializeFilterForm();
    this.toggleFilter();
    this.initializeUserInfo();
    this.getLogoImage();

    this.subscriptions.push(
      this.service.onTypeOrTabRefresh.subscribe(data => {
        this.selectedTab = data.selectedTab;
        if (this.selectedTab?.toLowerCase() === 'iteration') {
          this.service.setEmptyFilter();
        }
        this.projectIndex = 0;
        this.selectedType(data.selectedType);
      }),

      this.service.mapColorToProjectObs.subscribe((x) => {
        if (Object.keys(x).length > 0) {
          this.colorObj = x;
        }
      }),

      this.service.globalDashConfigData.subscribe(data => {
        this.kpiListData = data;
      }),

      this.service.passEventToNav.subscribe(() => {
        this.getNotification();
      }),

      this.service.iterationCongifData.subscribe((iterationDetails) => {
        this.iterationConfigData = iterationDetails;
      }),

      this.service.kpiListNewOrder.subscribe((kpiListNewOrder) => {
        this.kpisNewOrder = kpiListNewOrder;
      })
    );

    this.httpService.getTooltipData().subscribe((filterData) => {
      if (filterData[0] !== 'error') {
        this.heirarchyCount = filterData?.hierarchySelectionCount;
        this.dateRangeFilter = filterData?.dateRangeFilter;
        this.filterForm?.get('date')?.setValue(this.dateRangeFilter?.counts?.[0]);
      }
    });

     this.service.getLogoImage().subscribe((logoImage) => {
      this.getLogoImage();
    });
  }

  initializeFilterForm(){
    this.filterForm = new UntypedFormGroup({
      selectedTrendValue: new UntypedFormControl(),
      date: new UntypedFormControl(''),
      selectedLevel: new UntypedFormControl(),
      selectedSprintValue: new UntypedFormControl(),
      selectedRelease : new UntypedFormControl(),
    });
  }

  initializeUserInfo(){
    if (this.getAuthorizationService.checkIfSuperUser()) {
      this.isSuperAdmin = true;
    }
    // this.username = this.service.getCurrentUserDetails('user_name');

    let authoritiesArr;
    if (this.service.getCurrentUserDetails('authorities')) {
      authoritiesArr = this.service.getCurrentUserDetails('authorities');
    }
    if (authoritiesArr && authoritiesArr.includes('ROLE_GUEST')) {
      this.isGuest = true;
    }
    if (!this.isGuest) {
      this.items.unshift({
        label: 'Settings',
        icon: 'fa fa-cog',
        command: () => {
          this.service.setSideNav(false);
          this.router.navigate(['/dashboard/Config/']);
        },
      });
    }
  }

  toggleFilter(){
    // getting document click event from dashboard and check if it is outside click of the filter and if filter is open then closing it
    this.service.getClickedItem().subscribe((target) => {
      if (target && target !== this.toggleButton?.nativeElement && target?.closest('.kpi-dropdown') !== this.drpmenu?.nativeElement) {
        this.toggleDropdown = false;
      }
      if (Object.keys(this.toggleDropdownObj)?.length > 0) {
        for (const key in this.toggleDropdownObj) {
          const btn = document.getElementById(key + 'Btn');
          const dropdown = document.getElementById(key + 'DDn');
          if (target && target !== btn && target?.closest('.add-filters-dropdown') !== dropdown) {
            this.toggleDropdownObj[key] = false;
          }
        }
      }
      if (target && target !== this.dateToggleButton?.nativeElement && target?.closest('.date-filter-dropdown') !== this.dateDrpmenu?.nativeElement) {
        this.toggleDateDropdown = false;
      }
    });
  }


  /**create dynamic hierarchy levels for filter dropdown */
  setHierarchyLevels() {
    if(!this.hierarchies){
      this.httpService.getAllHierarchyLevels().subscribe((res) => {
        if (res.data) {
          this.hierarchies = res.data;
          localStorage.setItem('completeHierarchyData',JSON.stringify(this.hierarchies));
          this.setLevels();
          this.getFilterDataOnLoad();
        }
      });
    }else{
      this.setLevels();
      this.getFilterDataOnLoad();
    }
  }

  setLevels() {
    this.hierarchyLevels = [];
    this.additionalFiltersArr = [];
    const board = this.kanban ? 'kanban' : 'scrum';
    const projectLevel = this.hierarchies[board]?.filter((x) => x.hierarchyLevelId == 'project')[0]?.level;
    for (let i = 0; i < this.hierarchies[board]?.length; i++) {
      if (this.hierarchies[board][i]?.level <= projectLevel) {
        this.hierarchyLevels.push(this.hierarchies[board][i]);
      }
      if (this.hierarchies[board][i].level > projectLevel) {
        this.additionalFiltersArr.push(this.hierarchies[board][i]);
      }
    }
  }

  selectedType(type) {
    this.selectedFilterArray = [];
    this.tempParentArray = [];

    if(this.selectedTab?.toLowerCase() === 'iteration' || this.selectedTab?.toLowerCase() === 'backlog' || this.selectedTab?.toLowerCase() === 'maturity' || this.selectedTab?.toLowerCase() === 'release' || this.selectedTab?.toLowerCase() === 'mydashboard'){
      this.allowMultipleSelection = false;
    }else{
      this.allowMultipleSelection = true;
    }

    if (type.toLowerCase() === 'kanban') {
      this.kanban = true;
    } else {
      this.kanban = false;
    }

    if (this.kanban !== this.previousType) {
      this.filterForm?.reset();
      this.filterForm?.get('date')?.setValue(this.dateRangeFilter?.counts?.[0]);
    }

    const data = {
      url: this.router.url +'/' + (this.service.getSelectedType() ? this.service.getSelectedType() : 'Scrum'),
      userRole: this.getAuthorizationService.getRole(),
      version: this.httpService.currentVersion,
    };

    this.setHierarchyLevels();
    this.ga.setPageLoad(data);
    this.getKpiOrderedList();
  }

  makeUniqueArrayList(arr) {
    let uniqueArray = [];
    for (let i = 0; i < arr?.length; i++) {
      const idx = uniqueArray?.findIndex((x) => x.nodeId == arr[i]?.nodeId);
      if (idx == -1) {
        uniqueArray = [...uniqueArray, arr[i]];
        uniqueArray[uniqueArray?.length - 1]['path'] = [uniqueArray[uniqueArray?.length - 1]['path']];
        uniqueArray[uniqueArray?.length - 1]['parentId'] = [ uniqueArray[uniqueArray?.length - 1]['parentId']];
      } else {
        uniqueArray[idx].path = [...uniqueArray[idx]?.path, arr[i]?.path];
        uniqueArray[idx].parentId = [...uniqueArray[idx]?.parentId,arr[i]?.parentId];
      }
    }
    return uniqueArray;
  }

  getFilterDataOnLoad() {
    if (this.filterKpiRequest && this.filterKpiRequest !== '') {
      this.filterKpiRequest.unsubscribe();
    }
    this.selectedFilterData = {};
    this.selectedFilterCount = 0;
    this.selectedFilterData.kanban = this.kanban;
    this.selectedFilterData['sprintIncluded'] = !this.kanban ? ['CLOSED', 'ACTIVE'] : ['CLOSED'];
    const filterData = this.service.getFilterData();
    if (!Object.keys(filterData).length || this.previousType !== this.kanban) {
      this.filterKpiRequest = this.httpService.getFilterData(this.selectedFilterData).subscribe((filterApiData) => {
        this.processFilterData(filterApiData);
        this.previousType = this.kanban;
      });
    } else {
      this.processFilterData(filterData);
    }
  }

  processFilterData(filterData) {
    if (filterData[0] !== 'error') {
      this.filterData = filterData['data'];
      if (this.filterData.length == 0) {
        this.service.setNoProjects(true);
        this.initializeFilterForm();
        this.noProjects = true;
      }else{
        this.service.setNoProjects(false);
        this.noProjects = false;
      }
      this.service.setFilterData(JSON.parse(JSON.stringify(filterData)));
      /** check if data for additional filters exists in filterData api, if yes create a formControl for the same */
      this.additionalFiltersDdn = {};
      for (let i = 0; i < this.additionalFiltersArr?.length; i++) {
        let arr = this.filterData.filter((x) => x.labelName.toLowerCase() === this.additionalFiltersArr[i]['hierarchyLevelId']?.toLowerCase());
        if (arr?.length > 0) {
          arr = this.sortAlphabetically(arr);
          arr = this.makeUniqueArrayList(arr);
          this.additionalFiltersDdn[this.additionalFiltersArr[i]['hierarchyLevelId']] = arr;
          this.toggleDropdownObj[this.additionalFiltersArr[i]['hierarchyLevelId']] = false;
          if (this.additionalFiltersArr[i]['hierarchyLevelId'] == 'sprint') {
            this.createFormGroup(this.additionalFiltersArr[i]['hierarchyLevelId'],arr);
          } else {
            this.createFormGroup(this.additionalFiltersArr[i]['hierarchyLevelId']);
          }
        }
      }
      if(!this.noProjects){
        this.checkIfFilterAlreadySelected();
      }else{
        /** if no projects are present, reset project and release list */
        this.trendLineValueList = [];
        this.filteredAddFilters['release'] = [];
      }

      if (this.kanban) {
        this.selectedDateFilter = `${this.filterForm?.get('date')?.value} ${this.selectedDayType}`;
      }

      if (Object.keys(this.filterData).length !== 0) {
        this.disableDownloadBtn = false;
        this.getMasterData();
        this.service.setEmptyData(false);
      } else {
        this.service.setEmptyFilter();
        this.disableDownloadBtn = true;
        this.service.setEmptyData(true);
      }
    } else {
      this.service.setEmptyData(true);
    }
  }

  createFormGroup(level, arr?) {
    if (arr?.length > 0) {
      const obj = {};
      for (let i = 0; i < arr?.length; i++) {
        obj[arr[i]['nodeId']] = new UntypedFormControl(false);
      }
      this.filterForm.controls[level] = new UntypedFormGroup(obj);
    } else {
      this.filterForm.controls[level] = new UntypedFormControl('');
    }
  }

  getMasterData() {
    const masterData = this.service.getMasterData();
    if (!Object.keys(masterData).length) {
      this.httpService.getMasterData().subscribe((masterApiData) => {
        if (masterApiData[0] !== 'error') {
          this.service.setMasterData(JSON.parse(JSON.stringify(masterApiData)));
          this.processMasterData(masterApiData);
        }
      });
    } else {
      this.processMasterData(masterData);
    }
  }

  processMasterData(masterData) {
    this.masterData = masterData;
    if (this.selectedTab?.toLowerCase() === 'iteration') {
      this.projectIndex = 0;
        this.handleIterationFilters('project');
    }else if(this.selectedTab?.toLowerCase() === 'release'){
      this.projectIndex = 0;
        this.handleMilestoneFilter('project');
    }else  {
      this.applyChanges();
    }
  }

  assignUserNameForKpiData() {
    if (!this.kpiListData['username']) {
      delete this.kpiListData['id'];
    }
    this.kpiListData['username'] = this.service.getCurrentUserDetails('user_name');
  }

  closeAllDropdowns() {
    for (const key in this.toggleDropdownObj) {
      this.toggleDropdownObj[key] = false;
    }
  }

  filterAdditionalFilters() {
    this.filteredAddFilters = {};
    const selectedLevel = this.filterForm.get('selectedLevel')?.value;
    if (selectedLevel == 'project') {
      const selectedProjects = this.filterForm?.get('selectedTrendValue')?.value;
      for (const key in this.additionalFiltersDdn) {
        this.filteredAddFilters[key] = [];
      }
      if(selectedProjects?.length > 0){

        for (let i = 0; i < selectedProjects?.length; i++) {
          for (const key in this.additionalFiltersDdn) {
            if (key == 'sprint') {
              this.filteredAddFilters[key] = [...this.filteredAddFilters[key], ...this.additionalFiltersDdn[key]?.filter((x) => x['parentId']?.includes(selectedProjects[i]) &&  x['sprintState']?.toLowerCase() == 'closed')];
            } else {
              this.filteredAddFilters[key] = [...this.filteredAddFilters[key],...this.additionalFiltersDdn[key]?.filter((x) =>x['path'][0]?.includes(selectedProjects[i]))];
            }
          }
        }
      }
    }
  }

  onSelectedTrendValueChange($event) {
    this.additionalFiltersArr.forEach((additionalFilter) => {
    this.filterForm.get(additionalFilter['hierarchyLevelId'])?.reset();
    });
    this.applyChanges();
  }

  // this method would be called on click of apply button of filter
  applyChanges(applySource?, filterApplied = true): void {
    let selectedLevelId = this.filterForm?.get('selectedLevel')?.value;
    let selectedTrendIds = this.filterForm?.get('selectedTrendValue')?.value;
    let selectedLevel = this.hierarchyLevels?.filter((x) => x.hierarchyLevelId === selectedLevelId)[0];
    if (selectedTrendIds !== '' || selectedTrendIds?.length > 0) {
      let selectedTrendValues:any = [];

      if (Array.isArray(selectedTrendIds)) {
        for (let i = 0; i < selectedTrendIds?.length; i++) {
          selectedTrendValues.push(
            this.trendLineValueList?.filter( (x) => x.nodeId === selectedTrendIds[i] )[0]);
        }
      }else{
        selectedTrendValues.push(this.trendLineValueList?.filter((x) => x.nodeId == selectedTrendIds)[0]);
      }

      this.service.setSelectedLevel(selectedLevel);
      this.service.setSelectedTrends(selectedTrendValues);
      if (!applySource) {
        this.ngselect?.close();
        this.ngselect?.blur();
      } else {
        this.closeAllDropdowns();
      }
      /**push selected upper level hierarchy in selectedFilterArray */
      this.selectedFilterArray = [...selectedTrendValues];
      for (let i = 0; i < this.selectedFilterArray?.length; i++) {
        this.selectedFilterArray[i]['additionalFilters'] = [];
      }
      this.selectedFilterArray = this.sortAlphabetically(this.selectedFilterArray);
      /** add additional filters like sprints, date etc in selectedFilterArray */
      const isAdditionalFilter = this.additionalFiltersArr?.filter((x) => x['hierarchyLevelId'] == applySource || this.filterForm.get(x['hierarchyLevelId']));
      if (isAdditionalFilter?.length > 0) {
        for (let i = 0;i < Object.keys(this.additionalFiltersDdn)?.length;i++ ) {
          const additionalFilterFormVal = this.filterForm?.get(Object.keys(this.additionalFiltersDdn)[i])?.value;
          if (additionalFilterFormVal) {
            if (
              typeof additionalFilterFormVal === 'object' && Object.keys(additionalFilterFormVal)?.length > 0) {
              const selectedAdditionalFilter = this.additionalFiltersDdn[Object.keys(this.additionalFiltersDdn)[i]]?.filter((x) => additionalFilterFormVal[x['nodeId']] == true);
              for (let j = 0; j < selectedAdditionalFilter?.length; j++) {
                const parentNodeIdx = this.selectedFilterArray?.findIndex((x) => x.nodeId == selectedAdditionalFilter[j]['parentId'][0]);
                if (parentNodeIdx >= 0) {
                  this.selectedFilterArray[parentNodeIdx]['additionalFilters'] =
                    [...this.selectedFilterArray[parentNodeIdx]['additionalFilters'],selectedAdditionalFilter[j]];
                }
              }
            } else {
              const selectedAdditionalFilter = this.additionalFiltersDdn[Object.keys(this.additionalFiltersDdn)[i]]?.filter((x) => x['nodeId'] == additionalFilterFormVal)[0];
              const parentNodeIdx = this.selectedFilterArray?.findIndex((x) => selectedAdditionalFilter['path'][0]?.includes(x.nodeId));
              if (parentNodeIdx >= 0) {
                this.selectedFilterArray[parentNodeIdx]['additionalFilters'] = [...this.selectedFilterArray[parentNodeIdx]['additionalFilters'],selectedAdditionalFilter];
              }
            }
          }
        }
      }

      if (!applySource) {
        this.filterAdditionalFilters();
      }
      if (applySource?.toLowerCase() == 'date' && this.kanban) {
        this.selectedDateFilter = `${this.filterForm?.get('date')?.value} ${this.selectedDayType}`;
        this.service.setSelectedDateFilter(this.selectedDayType);
        this.toggleDateDropdown = false;
      }
      this.createFilterApplyData();
      this.setMarker();
      let isAdditionalFilters = false;
      for (const key in this.additionalFiltersDdn) {
        if (key != 'sprint' && this.filterForm.get(key)?.value) {
          isAdditionalFilters = true;
        }
      }
      this.service.select(this.masterData, this.filterData, this.filterApplyData, this.selectedTab, isAdditionalFilters, filterApplied,);
    }
  }

  createFilterApplyData() {
    this.resetFilterApplyObj();
    let isAdditionalFilterFlag = this.selectedFilterArray?.filter((item) => item?.additionalFilters?.length > 0)?.length > 0? true : false;
    for (let i = 0; i < this.selectedFilterArray?.length; i++) {
      if (isAdditionalFilterFlag) {
        const temp = this.selectedFilterArray[i]?.additionalFilters;
        for (let j = 0; j < temp?.length; j++) {
          if (this.filterApplyData['level'] < temp[j].level) {
            this.filterApplyData['level'] = temp[j].level;
            this.filterApplyData['selectedMap'][temp[j].labelName]?.push(temp[j].nodeId);
            this.filterApplyData['ids'] = [];
            this.filterApplyData['ids'].push(temp[j].nodeId);
          } else if (this.filterApplyData['level'] == temp[j].level) {
            this.filterApplyData['selectedMap'][temp[j].labelName]?.push(temp[j].nodeId);
            this.filterApplyData['ids'].push(temp[j].nodeId);
          }
          this.filterApplyData['label'] = temp[j]?.labelName;
          if (temp[j].labelName != 'sprint' || this.filterApplyData['selectedMap']['sprint']?.length == 0) {
            this.filterApplyData['selectedMap']['project'].push(this.selectedFilterArray[i]?.nodeId);
          }
        }
      } else {
        this.filterApplyData['level'] = this.selectedFilterArray[i]?.level;
        this.filterApplyData['selectedMap'][this.selectedFilterArray[i]?.labelName].push(this.selectedFilterArray[i]?.nodeId);
        this.filterApplyData['ids'].push(this.selectedFilterArray[i]?.nodeId);
        this.filterApplyData['label'] = this.selectedFilterArray[i]?.labelName;
      }
    }

    this.filterApplyData['sprintIncluded'] = this.selectedTab?.toLowerCase() == 'iteration'? ['CLOSED', 'ACTIVE']: ['CLOSED'];
    const dateFilter = this.filterForm?.get('date')?.value;
    if (dateFilter != '' && this.kanban) {
      this.filterApplyData['ids'] = [];
      this.filterApplyData['selectedMap']['date']?.push(this.selectedDayType.toUpperCase());
      this.filterApplyData['ids'].push(this.filterForm?.get('date')?.value);
      this.selectedDateFilter = `${this.filterForm?.get('date')?.value} ${this.selectedDayType}`;
    }
  }

  checkIfMaturityTabHidden(){
    const maturityBoard = this.kpiListData['others']?.find((board) =>
        board.boardName === 'Kpi Maturity');
    return (maturityBoard && maturityBoard.kpis[0].shown)  ? false : true;
  }

  navigateToSelectedTab() {
    if (this.selectedTab !== 'Config' && Object.keys(this.kpiListData)?.length > 0) {
      if (this.selectedTab === 'Maturity') {
        if (!this.checkIfMaturityTabHidden()) {
          this.router.navigateByUrl(
            `/dashboard/Maturity`,
          );
          return;
        } else {
          this.selectedTab = 'iteration';
          this.kanban = false;
        }
      }
      let boardDetails = this.kpiListData[this.kanban ? 'kanban' : 'scrum']?.find((board) =>board.boardName.toLowerCase() === this.selectedTab.toLowerCase() || board.boardName.toLowerCase() === this.selectedTab.toLowerCase().split('-').join(' ')) ||
        this.kpiListData['others']?.find((board) => board.boardName.toLowerCase() === this.selectedTab.toLowerCase());
      if (!boardDetails && this.kpiListData[this.kanban ? 'kanban' : 'scrum']?.length > 0) {
        boardDetails = this.kpiListData['scrum'].find(boardDetail => boardDetail.boardName.toLowerCase() === 'iteration');
      }
      this.selectedTab = boardDetails?.boardName;
        this.router.navigateByUrl(`/dashboard/${boardDetails?.boardName.split(' ').join('-').toLowerCase()}`);
    }
  }

  navigateToHomePage(){
    const previousSelectedTab = this.router.url.split('/')[2];
    if (previousSelectedTab === 'Config' || previousSelectedTab === 'Help') {
      this.kanban = false;
      this.selectedTab = 'iteration';
      this.service.setEmptyFilter();
      this.service.setSelectedType('scrum');
      this.projectIndex = 0;
      this.router.navigateByUrl(`/dashboard/iteration`);
    }
  }

  /** get kpi ordered list starts */
  get kpiFormValue() {
    return this.kpiForm.controls;
  }

  getKpiOrderedList() {
    if (this.isEmptyObject(this.kpiListData)) {
      this.httpService.getShowHideKpi().subscribe(
        (response) => {
          if (response.success === true) {
            this.kpiListData = response.data;
            this.service.setDashConfigData(this.kpiListData);
            this.processKpiList();
            this.navigateToSelectedTab();
          }
        },
        (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error in fetching roles. Please try after some time.',
          });
        },
      );
    } else {
      this.processKpiList();
      this.navigateToSelectedTab();
    }
  }

  processKpiList() {
    if (!this.isEmptyObject(this.kpiListData)) {
      switch (this.selectedTab.toLowerCase()) {
        case 'iteration':
          this.kpiList = this.kpiListData['scrum'].filter((item) => item.boardName.toLowerCase() == 'iteration')[0]?.kpis.filter((kpi) => kpi.kpiId !== 'kpi121');
          break;
        case 'backlog':
          this.kpiList = this.kpiListData['others'].filter((item) => item.boardName.toLowerCase() == 'backlog')?.[0]?.kpis;
          break;
        case 'release':
          this.kpiList = this.kpiListData['others'].filter((item) => item.boardName.toLowerCase() == 'release')?.[0]?.kpis;
          break;
        default:
          this.kpiList = this.kpiListData[this.kanban ? 'kanban' : 'scrum'].filter((item) => item.boardName.toLowerCase() === this.selectedTab.toLowerCase() || item.boardName.toLowerCase() === this.selectedTab.toLowerCase().split('-').join(' '))[0]?.kpis;
          break;
      }
      const kpiObj = {};
      let count = 0;
      this.showKpisList = [];
      for (let i = 0; i < this.kpiList?.length; i++) {
        let showKpi = false;
        if (this.kpiList[i]['shown']) {
          if (this.kpiList[i]['isEnabled']) {
            showKpi = true;
          } else {
            showKpi = false;
          }
          if (!showKpi) {
            count++;
          }
          kpiObj[this.kpiList[i]['kpiId']] = new UntypedFormControl(showKpi);
          this.showKpisList.push(this.kpiList[i]);
        }
      }
      if (this.showKpisList && this.showKpisList?.length > 0) {
        this.noAccessMsg = false;
        this.kpiForm = new UntypedFormGroup({
          enableAllKpis: new UntypedFormControl(count > 0 ? false : true),
          kpis: new UntypedFormGroup(kpiObj),
        });
      } else {
        this.noAccessMsg = true;
      }
    }
  }
  handleAllKpiChange(event) {
    const kpiObj = {};
    for (let i = 0; i < this.showKpisList.length; i++) {
      kpiObj[this.showKpisList[i]['kpiId']] = event.checked;
    }
    this.kpiFormValue['kpis'].setValue(kpiObj);
  }
  handleKpiChange(event) {
    if (!event.checked) {
      this.kpiFormValue['enableAllKpis'].setValue(false);
    } else {
      let checkIfAllKpiEnabled = true;
      for (const kpi in this.kpiFormValue?.kpis['controls']) {
        if (!this.kpiFormValue.kpis['controls'][kpi]['value']) {
          checkIfAllKpiEnabled = false;
          break;
        }
      }
      this.kpiFormValue['enableAllKpis'].setValue(checkIfAllKpiEnabled);
    }
  }
  submitKpiConfigChange() {
    for (let i = 0; i < this.kpiList.length; i++) {
      this.kpiList[i]['isEnabled'] =
        this.kpiFormValue['kpis'].value[this.kpiList[i]['kpiId']];
    }
    const kpiArray = this.kpiListData[this.kanban ? 'kanban' : 'scrum'];
    for (let i = 0; i < kpiArray.length; i++) {
      if (kpiArray[i].boardName.toLowerCase() == this.selectedTab.toLowerCase()) {
        if (this.selectedTab.toLowerCase() === 'iteration') {
          this.kpiListData[this.kanban ? 'kanban' : 'scrum'][i]['kpis'] = [this.kpiListData[this.kanban ? 'kanban'
          : 'scrum'][i]['kpis'].find((kpi) => kpi.kpiId === 'kpi121'),...this.kpiList];
        } else {
          this.kpiListData[this.kanban ? 'kanban' : 'scrum'][i]['kpis'] = this.kpiList;
        }
      }
    }
    this.assignUserNameForKpiData();
    this.httpService.submitShowHideKpiData(this.kpiListData).subscribe(
      (response) => {
        if (response.success === true) {
          this.messageService.add({
            severity: 'success',
            summary: 'Successfully Saved',
            detail: '',
          });
          this.service.setDashConfigData(this.kpiListData);
          this.toggleDropdown = false;
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Error in Saving Configuraion',
          });
        }
      },
      (error) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error in saving kpis. Please try after some time.',
        });
      },
    );
  }
  /** get kpi ordered list ends */


  setKPIOrder() {
    const kpiArray = this.selectedTab.toLowerCase() === 'release' ? this.kpiListData['others'] : this.kpiListData[this.kanban ? 'kanban' : 'scrum'];
    for (const kpiBoard of kpiArray) {
      if (kpiBoard.boardName.toLowerCase() === this.selectedTab.toLowerCase()) {
        kpiBoard.kpis = this.kpisNewOrder;
      }
    }
    this.kpiList = this.kpisNewOrder.filter((kpi) => kpi.kpiId !== 'kpi121');
    this.httpService.submitShowHideKpiData(this.kpiListData).subscribe(
      (response) => {
        this.kpisNewOrder = [];
        if (response.success === true) {
          this.messageService.add({
            severity: 'success',
            summary: 'Successfully Saved',
            detail: '',
          });
          this.service.setDashConfigData(this.kpiListData);
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Error in Saving Configuraion',
          });
        }
      },
      (error) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error in saving kpis. Please try after some time.',
        });
      },
    );
  }

  showTooltip(val) {
    this.isTooltip = val;
  }

  ngOnDestroy() {
    this.filterApplyData = [];
    this.service.setEmptyFilter();
    this.service.setSelectedLevel({});
    this.service.setSelectedTrends([]);
    this.service.setSelectedTab('');
    this.service.setFilterData({});
    this.service.setDashConfigData(null);
    this.service.selectedtype='';
    this.initializeFilterForm();
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
  }

  isEmptyObject(value) {
    return Object.keys(value).length === 0 && value.constructor === Object;
  }
  handleSelect(event) {
    this.trendLineValueList = this.filterData?.filter((x) => x.labelName?.toLowerCase() == event?.toLowerCase());
    this.trendLineValueList = this.sortAlphabetically(this.trendLineValueList);
    this.trendLineValueList = this.makeUniqueArrayList(this.trendLineValueList);
    this.filterForm?.get('selectedTrendValue').setValue('');
    this.service.setSelectedLevel(this.hierarchyLevels.find(hierarchy => hierarchy.hierarchyLevelId === event?.toLowerCase()));
  }

  setMarker() {
    let colorsArr = ['#079FFF','#cdba38', '#00E6C3', '#fc6471', '#bd608c', '#7d5ba6']
    const colorObj = {};
    for (let i = 0; i < this.selectedFilterArray?.length; i++) {
      colorObj[this.selectedFilterArray[i].nodeId] = { nodeName: this.selectedFilterArray[i].nodeName, color: colorsArr[i] }
    }
    this.service.setColorObj(colorObj);
  }

  resetFilterApplyObj() {
    this.filterApplyData = {
      ids: [],
      sprintIncluded: this.selectedTab?.toLowerCase() != 'iteration' ? ['CLOSED'] : ['CLOSED', 'ACTIVE'],
      selectedMap: {},
      level: 0,
      label : ''
    };
    for (let i = 0; i < this.hierarchyLevels?.length; i++) {
      this.filterApplyData['selectedMap'][this.hierarchyLevels[i]?.hierarchyLevelId] = [];
    }
    for (let i = 0; i < this.additionalFiltersArr?.length; i++) {
      this.filterApplyData['selectedMap'][this.additionalFiltersArr[i]['hierarchyLevelId']] = [];
    }
    if (this.kanban) {
      this.filterApplyData['selectedMap']['date'] = [];
    }
  }

  sortAlphabetically(objArray) {
    objArray?.sort((a, b) => a.nodeName?.localeCompare(b.nodeName));
    return objArray;
  }

  getTrendLevelArray() {
    return this.filterForm?.controls['selectedLevel']?.value?.toLowerCase();
  }

  setTrendValueFilter() {
    if(this.allowMultipleSelection){
      this.filterForm?.get('selectedTrendValue').setValue([this.trendLineValueList[0]['nodeId']]);
    }else{
      this.filterForm?.get('selectedTrendValue').setValue(this.trendLineValueList[0]['nodeId']);
    }
  }

  checkIfFilterAlreadySelected() {
    const selectedLevel = this.service.getSelectedLevel();
    const selectedTrends = this.service.getSelectedTrends();

    if (Object.keys(selectedLevel).length > 0 && selectedTrends.length > 0) {
      if (this.selectedTab.toLowerCase() === 'iteration' || this.selectedTab.toLowerCase() === 'backlog' || this.selectedTab.toLowerCase() === 'release') {
        if (this.previousType || selectedLevel['hierarchyLevelId'] !== 'project') {
               this.findProjectWhichHasData();
        } else {
          this.defaultFilterSelection = false;
          this.filterForm?.get('selectedLevel').setValue(selectedLevel['hierarchyLevelId']);
          const selectedTrendValue = this.allowMultipleSelection ? selectedTrends.map(selectedtrend => selectedtrend['nodeId']) : selectedTrends[0]['nodeId'];
          this.filterForm.get('selectedTrendValue').setValue(selectedTrendValue);
        }
      } else {
        if (this.previousType === this.kanban) {
          this.filterForm?.get('selectedLevel').setValue(selectedLevel['hierarchyLevelId']);
          const selectedTrendValue = this.allowMultipleSelection ? selectedTrends.map(selectedtrend => selectedtrend['nodeId']) : selectedTrends[0]['nodeId'];
          this.filterForm.get('selectedTrendValue').setValue(selectedTrendValue);
        } else {
          this.checkDefaultFilterSelection();
        }
      }
    } else {
      if (this.selectedTab.toLowerCase() === 'iteration' || this.selectedTab.toLowerCase() === 'backlog' || this.selectedTab.toLowerCase() === 'release') {
        this.findProjectWhichHasData();
      }else{
        this.checkDefaultFilterSelection();
      }
    }
  }

  checkDefaultFilterSelection() {
    this.defaultFilterSelection = true;
    this.filterForm?.get('selectedLevel').setValue('project');
    this.trendLineValueList = this.filterData?.filter((x) => x.labelName?.toLowerCase() === 'project');

    if (this.trendLineValueList?.length > 0) {
      this.trendLineValueList = this.sortAlphabetically(this.trendLineValueList);
      this.trendLineValueList = this.makeUniqueArrayList(this.trendLineValueList);
      this.setTrendValueFilter();
      this.service.setSelectedLevel(this.hierarchyLevels[this.hierarchyLevels.length - 1]);
      this.service.setSelectedTrends([this.trendLineValueList[0]]);
    } else {
      this.filterForm?.get('selectedTrendValue').setValue('');
    }
  }

  handleRemove() {
    this.ngselect.open();
  }

  handleClose() {
    this.ngselect.close();
  }

  isAddFilterDisabled(hierarchyLevelId) {
    const isProject = this.filterForm?.get('selectedLevel')?.value?.toLowerCase() == 'project';
    let isDisabled = false;
    let projectSelected = 0;

    if (hierarchyLevelId == 'sprint' && !this.kanban && isProject) {
      projectSelected = this.selectedFilterArray?.length;
    }
    if (hierarchyLevelId != 'sprint' && isProject) {
      projectSelected = this.selectedFilterArray?.filter((selectedFilter) => selectedFilter?.labelName === 'project')?.length;
    }

    if (hierarchyLevelId == 'sprint') {
      isDisabled = !isProject || !this.filteredAddFilters[hierarchyLevelId] || this.filteredAddFilters[hierarchyLevelId]?.length == 0 || (isProject && projectSelected == 0);
    } else {
      isDisabled = !isProject || (isProject && projectSelected !== 1) || !this.filteredAddFilters[hierarchyLevelId] || this.filteredAddFilters[hierarchyLevelId]?.length == 0;
    }
    return isDisabled;
  }

  findProjectWhichHasData() {
    this.defaultFilterSelection = true;
    this.filterForm?.get('selectedLevel').setValue('project');
    this.trendLineValueList = this.filterData?.filter((x) => x.labelName?.toLowerCase() === 'project');
    let projectIndex = 0;
    if (this.trendLineValueList?.length > 0) {
      this.trendLineValueList = this.sortAlphabetically(this.trendLineValueList);
      this.trendLineValueList = this.makeUniqueArrayList(this.trendLineValueList);

      for (let i = 0; i < this.trendLineValueList.length; i++) {
        projectIndex = i;
        this.selectedProjectData = this.trendLineValueList[projectIndex];
        if(this.selectedTab?.toLowerCase() === 'release'){
          this.checkIfProjectHasRelease();
          if (Object.keys(this.selectedRelease).length > 0) {
            break;
          }
        }else{
          this.checkIfProjectHasData();
          if (Object.keys(this.selectedSprint).length > 0) {
            break;
          }
        }
      }
      if (projectIndex < this.trendLineValueList?.length) {
        this.filterForm?.get('selectedTrendValue')?.setValue(this.trendLineValueList[projectIndex]?.nodeId);
        this.filterForm.get('selectedSprintValue').setValue(this.selectedSprint['nodeId']);
      } else {
        this.projectIndex = 0;
        this.filterForm?.get('selectedTrendValue')?.setValue(this.trendLineValueList[this.projectIndex]?.nodeId);
      }
      this.service.setSelectedLevel(this.hierarchyLevels.find(hierarchy => hierarchy.hierarchyLevelId === 'project'));
      this.service.setSelectedTrends([this.trendLineValueList.find(trend => trend.nodeId === this.filterForm?.get('selectedTrendValue')?.value)]);
    }
  }

  checkIfProjectHasData(){
    let activeSprints = [];
    let closedSprints = [];
    this.selectedSprint={};
    const selectedProject = this.selectedProjectData['nodeId'];
    this.filteredAddFilters['sprint'] = [];
    if (this.additionalFiltersDdn && this.additionalFiltersDdn['sprint']) {
      this.filteredAddFilters['sprint'] = [...this.additionalFiltersDdn['sprint']?.filter((x) => x['parentId']?.includes(selectedProject))];
    }
    activeSprints = [...this.filteredAddFilters['sprint']?.filter((x) => x['sprintState']?.toLowerCase() == 'active')];
    closedSprints = [...this.filteredAddFilters['sprint']?.filter((x) => x['sprintState']?.toLowerCase() == 'closed')];
    if (activeSprints?.length > 0) {
      this.selectedSprint = { ...activeSprints[0] };
    } else if (closedSprints?.length > 0) {
      this.selectedSprint = closedSprints[0];
      for (let i = 0; i < closedSprints?.length; i++) {
        const sprintEndDateTS1 = new Date(closedSprints[i]['sprintEndDate']).getTime();
        const sprintEndDateTS2 = new Date(this.selectedSprint['sprintEndDate']).getTime();
        if (sprintEndDateTS1 > sprintEndDateTS2) {
          this.selectedSprint = closedSprints[i];
        }
      }
    } else {
      this.selectedFilterArray = [];
      this.selectedSprint={};
      this.service.setNoSprints(true);
    }
  }

  /*'type' argument: to understand onload or onchange
    1: onload
    2: onchange */
  handleIterationFilters(level) {
    if (this.filterForm?.get('selectedTrendValue')?.value != '') {
      this.service.setNoSprints(false);
      if (level?.toLowerCase() === 'project') {
        const selectedProject = this.filterForm?.get('selectedTrendValue')?.value;
        this.filterForm?.get('selectedSprintValue')?.setValue('');
        this.selectedProjectData = this.trendLineValueList.find(x => x.nodeId === selectedProject);
        this.checkIfProjectHasData();
        this.filterForm.get('selectedSprintValue').setValue(this.selectedSprint['nodeId']);
      }

      if (level?.toLowerCase() == 'sprint') {
        const val = this.filterForm.get('selectedSprintValue').value;
        this.selectedSprint = {...this.filteredAddFilters['sprint']?.filter((x) => x['nodeId'] == val)[0]};
      }
      if(this?.selectedProjectData){
        this.getProcessorsTraceLogsForProject(this?.selectedProjectData['basicProjectConfigId']);
      }
      this.service.setSelectedLevel(this.hierarchyLevels.find(hierarchy => hierarchy.hierarchyLevelId === 'project'));
      this.service.setSelectedTrends([this.trendLineValueList.find(trend => trend.nodeId === this.filterForm?.get('selectedTrendValue')?.value)]);
      if (this.selectedSprint && Object.keys(this.selectedSprint)?.length > 0) {
        this.selectedFilterArray = [];
        this.selectedFilterArray.push(this.selectedSprint);
        this.createFilterApplyData();
        this.service.select(this.masterData,this.filterData,this.filterApplyData,this.selectedTab);
      }
    }
  }

  getDate(type) {
    if (this.selectedTab.toLowerCase() === 'iteration') {
      return this.getFormatDateBasedOnIterationAndMilestone(type, 'sprint', "selectedSprintValue", "sprintStartDate", "sprintEndDate");
    } else {
      return this.getFormatDateBasedOnIterationAndMilestone(type, 'release', "selectedRelease", "releaseStartDate", "releaseEndDate");
    }
  }

  /** Get formated start/end date for Iteration and Milestone   */
  getFormatDateBasedOnIterationAndMilestone(type,filteredAddFiltersKey,formfield,startDateField,endDateField){
    let dateString = 'N/A';
    const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const selectedField = this.filterForm?.get(formfield)?.value;
      if (selectedField) {
        const obj = this.filteredAddFilters[filteredAddFiltersKey]?.filter((x) => x['nodeId'] == selectedField)[0];

        if(obj && (obj[startDateField] === '' && type === 'start') || (obj[endDateField] === '' && type === 'end')) {
          return dateString;
        }

        if (obj) {
          let d;
          if (type == 'start') {
            d = new Date(obj[startDateField]);
          } else {
            d = new Date(obj[endDateField]);
          }
          dateString = [this.pad(d.getDate()),this.pad(monthNames[d.getMonth()]),d.getFullYear()].join('/');
        }
      }

    return dateString;
  }

  pad(s) {
    return s < 10 ? '0' + s : s;
  }

  startDateSelected(val) {
    this.beginningDate = new Date(val);
  }

  removeItem(hierarchyLevelId, nodeId) {
    const hierarchy = this.filterForm.get(hierarchyLevelId);
    if (typeof hierarchy?.value === 'object' && Object.keys(hierarchy?.value)?.length > 0) {
      this.filterForm.get(hierarchyLevelId).get(nodeId).setValue(false);
    } else {
      this.filterForm.get(hierarchyLevelId).setValue('');
    }

    this.applyChanges(hierarchyLevelId);
  }

  removeNode(nodeId: string) {
    this.selectedFilterArray = this.selectedFilterArray.filter((node) => node.nodeId !== nodeId);
    const selectedNode = this.selectedFilterArray.map((node) => node.nodeId);
    this.filterForm.get('selectedTrendValue').setValue(selectedNode);
    this.applyChanges(null, false);
  }

  getProcessorsTraceLogsForProject(basicProjectConfigId) {
    this.httpService.getProcessorsTraceLogsForProject(basicProjectConfigId).subscribe((response) => {
        if (response.success) {
          if(this.selectedProjectData && this.selectedProjectData['basicProjectConfigId'] === basicProjectConfigId){
            this.processorsTracelogs = response.data;
        }
          this.showExecutionDate();
        } else {
          this.messageService.add({
            severity: 'error',
            summary:
              "Error in fetching processor's execution date. Please try after some time.",
          });
        }
      });
  }

  findTraceLogForTool() {
    return this.processorsTracelogs.find((ptl) => this.processorName.includes(ptl['processorName'].toLowerCase()));
  }

  showExecutionDate() {
    this.selectedProjectLastSyncDetails = this.findTraceLogForTool();
    if(this.selectedProjectLastSyncDetails != undefined && this.selectedProjectLastSyncDetails != null){
      if(this.selectedProjectLastSyncDetails.executionSuccess){
        this.selectedProjectLastSyncDate = this.selectedProjectLastSyncDetails.executionEndedAt;
        this.selectedProjectLastSyncStatus = "SUCCESS";
      }else{
        this.selectedProjectLastSyncDate = this.selectedProjectLastSyncDetails.executionEndedAt;
        this.selectedProjectLastSyncStatus = "FAILURE";
      }
    }else{
      this.selectedProjectLastSyncStatus = "";
      this.selectedProjectLastSyncDate = "NA"
   }
  }
  setSelectedDateType(label: string) {
    this.selectedDayType = label;
  }

  checkIfBtnDisabled(hierarchyLevelId) {
    let isDisabled = true;
    if (hierarchyLevelId === 'sprint') {
      for (const item in this.filterForm?.get(hierarchyLevelId)?.value) {
        if (this.filterForm?.get(hierarchyLevelId)?.value[item]) {
          isDisabled = null;
        }
      }
    } else {
      isDisabled = !this.filterForm?.get(hierarchyLevelId)?.value ? true : null;
    }
    return isDisabled;
  }

  getLevelName(id) {
    const name = this.hierarchyLevels?.filter((x) => x.hierarchyLevelId == id)[0]?.hierarchyLevelName;
    return name ? name : 'Project';
  }

  showChartToggle(val) {
    this.showChart = val;
    this.service.setShowTableView(this.showChart);
  }

  exportToExcel($event = null) {
    this.disableDownloadBtn = true;
    this.service.setGlobalDownload(true);
  }

  getNotification() {
    this.totalRequestCount  = 0 ;
    this.httpService.getAccessRequestsNotifications().subscribe((response: NotificationResponseDTO) => {
        if (response && response.success) {
          if (response.data?.length) {
            this.notificationList = [...response.data].map((obj) => {
                this.totalRequestCount = this.totalRequestCount + obj.count;
              return {
                label: obj.type + ' : ' + obj.count,
                icon: '',
                command: () => {
                  this.routeForAccess(obj.type);
                },
              };
            });
          }
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Error in fetching requests. Please try after some time.',
          });
        }
      });
  }

  // logout is clicked  and removing auth token , username
  logout() {
    this.httpService.logout().subscribe((getData) => {
      if (!(getData !== null && getData[0] === 'error')) {
        this.helperService.isKanban = false;
        localStorage.clear();
        // Set blank selectedProject after logged out state
        this.service.setSelectedProject(null);
        this.service.setCurrentUserDetails({});
        this.router.navigate(['./authentication/login']);
      }
    });
  }

  // when user would want to give access on project from notification list
  routeForAccess(type: string) {
    if (this.getAuthorizationService.checkIfSuperUser() || this.getAuthorizationService.checkIfProjectAdmin()) {
      switch (type) {
        case 'Project Access Request':
          this.service.setSideNav(false);
          this.router.navigate(['/dashboard/Config/Profile/GrantRequests']);
          break;
        case 'User Access Request':
          this.service.setSideNav(false);
          this.router.navigate(['/dashboard/Config/Profile/GrantNewUserAuthRequests']);
          break;
        default:
      }
    } else {
      this.router.navigate(['/dashboard/Config/Profile/RequestStatus']);
    }
  }

    /*Rendered the logo image */
    getLogoImage() {
      this.httpService.getUploadedImage().pipe(first()).subscribe((data) => {
          if (data['image']) {
            this.logoImage = 'data:image/png;base64,' + data['image'];
          } else {
            this.logoImage = undefined;
          }
        });
    }

  /** when user clicks on Back to dashboard or logo*/
   navigateToDashboard(){
    this.httpService.getShowHideKpi().subscribe(response =>{
      this.service.setDashConfigData(response.data);
      this.kpiListData = response.data;
      this.getNotification();
      this.selectedFilterData.kanban = this.kanban;
      this.selectedFilterData['sprintIncluded'] = !this.kanban ? ['CLOSED', 'ACTIVE'] : ['CLOSED'];
      this.httpService.getFilterData(this.selectedFilterData).subscribe((filterApiData) => {
        this.previousType = this.kanban;
        this.filterData = filterApiData['data'];
        const selectedLevel = this.service.getSelectedLevel();
        if(Object.keys(selectedLevel).length > 0){
          this.trendLineValueList = this.filterData?.filter((x) => x.labelName?.toLowerCase() ===selectedLevel['hierarchyLevelId'].toLowerCase());
          this.trendLineValueList = this.sortAlphabetically(this.trendLineValueList);
          this.trendLineValueList = this.makeUniqueArrayList(this.trendLineValueList);
        }
        this.service.setFilterData(JSON.parse(JSON.stringify(filterApiData)));
        const selectedTrends = this.service.getSelectedTrends();
        const selectedTrendNodeIds = selectedTrends.map(trend => trend.nodeId);
        const filteredTrendValue = this.trendLineValueList.filter(trend => selectedTrendNodeIds.includes(trend.nodeId));
        this.service.setSelectedTrends(filteredTrendValue);
        if(filteredTrendValue.length  === 0){
          this.checkIfFilterAlreadySelected();
        }
        this.navigateToSelectedTab();
      });
    });
   }

   getCurrentUserDetails(){
    this.httpService.getCurrentUserDetails().subscribe(details=>{
      if(details['success']){
        this.service.setCurrentUserDetails(details['data']);
      }
    });
   }
  handleMilestoneFilter(level) {
    const selectedProject = this.filterForm?.get('selectedTrendValue')?.value;
    this.filteredAddFilters['release'] = []
    if (this.additionalFiltersDdn && this.additionalFiltersDdn['release']) {
      this.filteredAddFilters['release'] = this.additionalFiltersDdn['release']?.filter((x) => x['parentId'][0]?.includes(selectedProject));
    }
    if (this.filteredAddFilters && this.filteredAddFilters['release'].length > 0) {
      if (level === 'project') {
        this.filterForm?.get('selectedRelease')?.setValue('');
        this.selectedProjectData = this.trendLineValueList.find(x => x.nodeId === selectedProject);
        this.checkIfProjectHasRelease();
        this.filterForm.get('selectedRelease').setValue(this.selectedRelease['nodeId']);
      }
      this.service.setNoRelease(false);
      this.selectedFilterArray = [];
      this.selectedFilterArray.push(this.filteredAddFilters['release'].filter(rel => rel['nodeId'] === this.filterForm.get('selectedRelease').value)[0]);
      this.createFilterApplyData();
      this.service.select(this.masterData, this.filterData, this.filterApplyData, this.selectedTab);
    } else {
      this.filterForm.controls['selectedRelease'].reset();
      this.service.setNoRelease(true);
    }
    this.service.setSelectedTrends([this.trendLineValueList.find(trend => trend.nodeId === this.filterForm?.get('selectedTrendValue')?.value)]);
  }

   checkIfProjectHasRelease(){
    let activeRelease = [];
    let closedRelease = [];
    this.selectedRelease={};
    const selectedProject = this.selectedProjectData['nodeId'];
    this.filteredAddFilters['release'] = [];
    if (this.additionalFiltersDdn && this.additionalFiltersDdn['release']) {
      this.filteredAddFilters['release'] = [...this.additionalFiltersDdn['release']?.filter((x) => x['parentId']?.includes(selectedProject))];
    }
    activeRelease = [...this.filteredAddFilters['release']?.filter((x) => x['releaseState']?.toLowerCase() == 'unreleased')];
    closedRelease = [...this.filteredAddFilters['release']?.filter((x) => x['releaseState']?.toLowerCase() == 'released')];
    if (activeRelease?.length > 0) {
      this.selectedRelease = { ...activeRelease[0] };
    } else if (closedRelease?.length > 0) {
      this.selectedRelease = closedRelease[0];
      for (let i = 0; i < closedRelease?.length; i++) {
        const releaseEndDateTS1 = new Date(closedRelease[i]['sprintEndDate']).getTime();
        const releaseEndDateTS2 = new Date(this.selectedRelease['sprintEndDate']).getTime();
        if (releaseEndDateTS1 > releaseEndDateTS2) {
          this.selectedRelease = closedRelease[i];
        }
      }
    } else {
      this.selectedFilterArray = [];
      this.selectedRelease={};
      this.service.setNoRelease(true);
    }
  }
}