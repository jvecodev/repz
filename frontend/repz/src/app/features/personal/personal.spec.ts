import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { Personal } from './personal';

describe('Personal', () => {
  let component: Personal;
  let fixture: ComponentFixture<Personal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Personal],
      providers: [provideRouter([]), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(Personal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
