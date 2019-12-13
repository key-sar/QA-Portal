import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { CvModel } from '../_common/models/cv.model';
import { ENTER } from '@angular/cdk/keycodes';
import * as _ from 'lodash';
import { MatChipInputEvent } from '@angular/material';
import { CvService } from '../_common/services/cv.service';
import { finalize } from 'rxjs/operators';
import { ViewCvService } from '../view-cv/services/view-cv.service';
import { IN_PROGRESS_STATUS, FAILED_REVIEW_STATUS, APPROVED_STATUS, FOR_REVIEW_STATUS } from '../view-cv/models/view-cv.constants';
import { Observable } from 'rxjs';
import { QaErrorHandlerService } from 'projects/portal-core/src/app/_common/services/qa-error-handler.service';
import { ViewCvStateManagerService } from '../view-cv/services/view-cv-state-manager.service';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { ADMIN_CV_SEARCH_URL } from '../_common/models/cv.constants';

@Component({
  selector: 'app-generate-cv',
  templateUrl: './generate-cv.component.html',
  styleUrls: ['./generate-cv.component.scss']
})
export class GenerateCvComponent implements OnInit {

  readonly separatorKeysCodes: number[] = [ENTER];

  public isLoading = false;

  public skillCategories = [
    {
      label: 'Programming Languages',
      key: 'programmingLanguages'
    },
    {
      label: 'IDEs',
      key: 'ides'
    },
    {
      label: 'Operating Systems',
      key: 'operatingSystems'
    },
    {
      label: 'Devops',
      key: 'devops'
    },
    {
      label: 'Databases',
      key: 'databases'
    },
    {
      label: 'Cloud Platforms',
      key: 'platforms'
    },
    {
      label: 'Other',
      key: 'other'
    },
  ];

  public cvForm: FormGroup;
  origCv: CvModel;
  public canComment = true;
  public isTraineeView = true;
  constructor(private router: Router, private activatedRoute: ActivatedRoute, private viewCvStateManagerService: ViewCvStateManagerService, private cvService: CvService, private errorHandlerService: QaErrorHandlerService) {


    const fb = new FormBuilder();

    this.cvForm = fb.group({
      firstName: [''],
      surname: [''],
      profile: fb.group({ profileDetails: ['', [Validators.required, Validators.maxLength(1800)]] }),
      skills: fb.group({
        programmingLanguages: [[]],
        ides: [[]],
        operatingSystems: [[]],
        devops: [[]],
        databases: [[]],
        platforms: [[]],
        other: [[]]
      }),
      hobbies: fb.group({ hobbiesDetails: ['', [Validators.required, Validators.maxLength(750)]] }),
      id: [[]],
      allQualifications: [[]],
      allWorkExperience: [[]],
      otherWorkExperience: [[]],
      sourceControlLink: ['']
    });
  }

  ngOnInit() {
    this.setRoleForPage();   // Is page being displayed for Trainee or Admin
    // this.cvForm.patchValue(new CvModel());
    // this.isTraineeView = this.viewCvStateManagerService.isPageDisplayForTrainee(this.activatedRoute);  // Is page being displayed for Trainee or Admin

    if (this.isTraineeView) {
      this.initialiseCvPageForTrainee();
    } else {
      this.initialiseCvPageForAdmin();
    };
  }

  private setRoleForPage() {
    this.isTraineeView = this.viewCvStateManagerService.isPageDisplayForTrainee(this.activatedRoute);
  }

  private initialiseCvPageForTrainee() {
    this.cvService.getCurrentCvForTrainee().subscribe(
      (cv) => {
        if (this.noExistingCvForTrainee(cv)) {
          //this.initialiseBlankCvForTrainee(); may not need to initialize new cv due to form format.
        } else {
          console.log(cv);
          this.origCv = cv;
          this.cvForm.patchValue({ ...cv, skills: _.get(cv, ['allSkills', '0'], {}) });
          this.refreshPageStatus();
        }
      },
      (error) => {
        this.processError(error);
      });
  }

  private initialiseCvPageForAdmin() {
    this.activatedRoute.paramMap.subscribe(
      (paramMap: ParamMap) => {
        this.cvService.getCvForId(paramMap.get('id')).subscribe(
          (cv) => {
            if (this.noExistingCvForTrainee(cv)) {
              //this.initialiseBlankCvForTrainee(); may not need to initialize new cv due to form format.
            } else {
              console.log(cv);
              this.origCv = cv;
              this.cvForm.patchValue({ ...cv, skills: _.get(cv, ['allSkills', '0'], {}) });
              this.refreshPageStatus();
            }
          },
          (error) => {
            this.processError(error);
          });
      });
  }

  private noExistingCvForTrainee(traineeCv: CvModel): boolean {
    return !traineeCv;
  }

  private processError(error: any) {
    //this.loadingData = false;
    this.errorHandlerService.handleError(error);
  }

  public addSkill(category, { value, input }: MatChipInputEvent): void {
    if (value) {
      this.cvForm.patchValue({
        skills: { [category]: [...this.cvForm.value.skills[category], value] }
      });
    }
    input.value = '';
  }

  public removeSkill(category, value): void {
    this.cvForm.patchValue({
      skills: { [category]: this.cvForm.value.skills[category].filter(v => v !== value) }
    });
  }

  // for generating, saving, downloading
  private getCvData(): CvModel {
    const { skills, id, ...rest } = this.cvForm.value;
    return _.merge(new CvModel(), {
      ...this.origCv,
      allSkills: [skills],
      fullName: `${rest.firstName} ${rest.surname}`,
      ...rest
    } as CvModel).build();
  }
  //Button Functions
  //All Users Buttons
  onGenerateCvButtonClicked() {
    this.cvForm.disable();
    this.isLoading = true;
    this.cvService.displayCvPdf(this.getCvData()).pipe(
      finalize(() => {
        this.cvForm.enable();
        this.isLoading = false;
      })
    ).subscribe(() => { });
  }

  onDownloadCvButtonClicked() {
    this.cvForm.disable();
    this.isLoading = true;
    this.cvService.downloadCvPdf(this.getCvData()).pipe(
      finalize(() => {
        this.cvForm.enable();
        this.isLoading = false;
      })
    ).subscribe(() => { });
  }
  //User Buttons
  onSaveCvButtonClicked() {
    const cvForm = this.getCvData();
    if (!cvForm.id) {
      cvForm.status = IN_PROGRESS_STATUS;
    }
    this.persistCvForTrainee(cvForm);
  }
  onSubmitCvButtonClicked() {
    const cvForm = this.getCvData();
    cvForm.status = FOR_REVIEW_STATUS;
    this.persistCvForTrainee(cvForm);
    //This needs to disable any further edits to the CV, wich it curently doesn't
  }
  onNewCvButtonClicked() {
    const cvForm = this.getCvData();
    cvForm.status = IN_PROGRESS_STATUS;
    this.createCv(cvForm);
  }
  //Admin Buttons
  onApproveCvButtonClicked() {
    const cvForm = this.getCvData();
    cvForm.status = APPROVED_STATUS;
    this.updateCv(cvForm);
    this.navigateToAdminSearch();
  }
  onFailCvButtonClicked() {
    const cvForm = this.getCvData();
    cvForm.status = FAILED_REVIEW_STATUS;
    this.updateCv(cvForm);
    this.navigateToAdminSearch();
  }
  private navigateToAdminSearch() {
    this.router.navigateByUrl(ADMIN_CV_SEARCH_URL);
  }

  // CV PERSIST FUNCTIONS
  private persistCvForTrainee(cvForm: CvModel) {
    if (!cvForm.id) {
      this.createCv(cvForm);
    } else {
      this.updateCv(cvForm);
    }
  }

  private createCv(cvForm: CvModel): void {
    this.processCvServiceResponse(this.cvService.createCv(cvForm));
  }

  private updateCv(cvForm: CvModel): void {
    this.processCvServiceResponse(this.cvService.updateCv(cvForm));
  }

  private processCvServiceResponse(obs: Observable<CvModel>) {
    this.cvForm.disable();
    this.isLoading = true;
    obs.pipe(
      finalize(() => {
        this.cvForm.enable();
        this.isLoading = false;
      })
    ).subscribe(
      (response) => {
        this.cvForm.patchValue({ ...response, skills: _.get(response, ['allSkills', '0'], {}) });
        this.setPageEditStatus();
      },
      (error) => {
        this.errorHandlerService.handleError(error);
      }
    );
  }



  // STATUS UPDATE FUNCTIONS

  private refreshPageStatus() {
    // this.setPageEditStatus();
    // this.setCommentStatus();
    // this.loadingData = false;
  }

  private setPageEditStatus(): void {
    // this.canEdit = this.viewCvStateManagerService.isPageEditable(this.activatedRoute, this.origCv);
  }

  private setCommentStatus() {
    // if (SubmitConfirmDialogComponent) {
    //   this.canComment = this.activatedRoute.snapshot.data.roles[0] === TRAINING_ADMIN_ROLE && this.origCv.status === FOR_REVIEW_STATUS;
    // }
  }

}