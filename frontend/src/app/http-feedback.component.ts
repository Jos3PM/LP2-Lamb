import { Component, inject } from '@angular/core';
import { HttpFeedbackService } from './http-feedback.service';

@Component({ selector: 'app-http-feedback', standalone: true, template: `@if (feedback.message()) { <div class="http-feedback" role="alert"><span>{{ feedback.message() }}</span><button type="button" (click)="feedback.clear()" aria-label="Cerrar aviso">×</button></div> }`, styles: [`.http-feedback{display:flex;justify-content:space-between;align-items:center;gap:1rem;padding:.85rem 1rem;border:1px solid #efc38a;border-radius:8px;background:#fff7e9;color:#704610;margin:1rem 0}.http-feedback button{border:0;background:transparent;font-size:1.2rem;cursor:pointer}`] })
export class HttpFeedbackComponent { readonly feedback = inject(HttpFeedbackService); }
