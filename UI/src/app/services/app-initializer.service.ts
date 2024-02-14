import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import { SharedService } from './shared.service';
import { HttpService } from './http.service';
import { ActivatedRoute, Router } from '@angular/router';
import { FeatureFlagsService } from './feature-toggle.service';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { GoogleAnalyticsService } from './google-analytics.service';
@Injectable({
  providedIn: 'root'
})
export class AppInitializerService {

  constructor(private sharedService: SharedService, private httpService: HttpService, private router: Router, private featureToggleService: FeatureFlagsService, private http: HttpClient, private route: ActivatedRoute, private ga: GoogleAnalyticsService) {
    this.checkFeatureFlag();
    this.validateToken();
  }

  async validateToken() {
    if (environment['AUTHENTICATION_SERVICE']) {
      let url = window.location.href;
      let authToken = url.split("authToken=")?.[1]?.split("&")?.[0];
      if (authToken) {
        this.sharedService.setAuthToken(authToken);
      }
      let obj = {
        'resource': environment.RESOURCE,
        'authToken': authToken
      };
      // Make API call or initialization logic here...
      this.httpService.getUserValidation(obj).subscribe((response) => {
        if (response?.['success']) {
          this.sharedService.setCurrentUserDetails(response?.['data']);
          localStorage.setItem("user_name", response?.['data']?.user_name);
          localStorage.setItem("user_email", response?.['data']?.user_email);
          const redirect_uri = localStorage.getItem('redirect_uri');
          if (authToken) {
            this.ga.setLoginMethod(response?.['data'], response?.['data']?.authType);
          }
          if (redirect_uri) {
            if (redirect_uri.startsWith('#')) {
              this.router.navigate([redirect_uri.split('#')[1]])
            } else {
              this.router.navigate([redirect_uri]);
            }
            localStorage.removeItem('redirect_uri');
          } else {
            this.router.navigate(['/dashboard/iteration'], { queryParamsHandling: 'merge' });
          }
        }
      }, error => {
        console.log(error);
      })

    }
  }

  async checkFeatureFlag() {
    // return new Promise((resolve, reject) => {
    if (!environment.production) {
      this.featureToggleService.config = this.featureToggleService.loadConfig().then((res) => res);;
    } else {
      const env$ = this.http.get('assets/env.json').pipe(
        tap(env => {
          environment['baseUrl'] = env['baseUrl'] || '';
          environment['SSO_LOGIN'] = env['SSO_LOGIN'] || false;
          environment['CENTRAL_LOGIN_URL'] = env['CENTRAL_LOGIN_URL'] || '';
          environment['AUTHENTICATION_SERVICE'] = env['AUTHENTICATION_SERVICE'] || true;
          environment['MAP_URL'] = env['MAP_URL'] || '';
          environment['RETROS_URL'] = env['RETROS_URL'] || '';
        }));

      env$.toPromise().then(async res => {
        this.featureToggleService.config = this.featureToggleService.loadConfig().then((res) => res);
      });
    }
    // load google Analytics script on all instances except local and if customAPI property is true
    let addGAScript = await this.featureToggleService.isFeatureEnabled('GOOGLE_ANALYTICS');
    if (addGAScript) {
      if (window.location.origin.indexOf('localhost') === -1) {
        this.ga.load('gaTagManager').then(data => {
          console.log('script loaded ', data);
        })
      }
    }
    // resolve(true);
    // })
  }
}

