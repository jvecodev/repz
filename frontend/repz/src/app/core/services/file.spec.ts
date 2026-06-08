import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FileService } from './file';

const BASE = 'http://localhost:8080/api/files';

describe('FileService', () => {
  let service: FileService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FileService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('minhaFoto faz GET /me e retorna URL', () => {
    let result: string | null | undefined;
    service.minhaFoto().subscribe((r) => (result = r));
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush('https://cdn.repz.com/foto.jpg');
    expect(result).toBe('https://cdn.repz.com/foto.jpg');
  });

  it('minhaFoto retorna null quando URL está vazia', () => {
    let result: string | null | undefined;
    service.minhaFoto().subscribe((r) => (result = r));
    http.expectOne(`${BASE}/me`).flush('   ');
    expect(result).toBeNull();
  });

  it('minhaFoto retorna null em caso de erro', () => {
    let result: string | null | undefined;
    service.minhaFoto().subscribe((r) => (result = r));
    http.expectOne(`${BASE}/me`).flush({}, { status: 404, statusText: 'Not Found' });
    expect(result).toBeNull();
  });

  it('upload faz POST com FormData', () => {
    const file = new File(['content'], 'foto.jpg', { type: 'image/jpeg' });
    let result: string | undefined;
    service.upload(file).subscribe((r) => (result = r));
    const req = http.expectOne((r) => r.method === 'POST' && r.url === `${BASE}/upload`);
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush('https://cdn.repz.com/novo.jpg');
    expect(result).toBe('https://cdn.repz.com/novo.jpg');
  });
});
