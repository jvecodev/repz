import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { Academia } from './academia';

describe('Academia', () => {
  let component: Academia;
  let fixture: ComponentFixture<Academia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Academia],
      providers: [provideRouter([]), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(Academia);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
