import { AfterViewInit, Component, ElementRef, OnDestroy, signal, viewChild } from '@angular/core';
import * as THREE from 'three';

/**
 * Escena 3D de la portada: un teclado de teclas que "respiran" en ola, un nudo de neón flotando,
 * una malla geométrica y partículas. Reacciona al mouse (parallax) y se pausa cuando no se ve.
 */
@Component({
  selector: 'app-escena3d',
  templateUrl: './escena3d.html',
  styleUrl: './escena3d.css',
})
export class Escena3d implements AfterViewInit, OnDestroy {
  private lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  protected sinWebgl = signal(false);

  private renderer?: THREE.WebGLRenderer;
  private frame = 0;
  private visible = true;
  private limpiar: Array<() => void> = [];

  ngAfterViewInit() {
    const canvas = this.lienzo().nativeElement;
    try {
      this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: 'high-performance' });
    } catch {
      this.sinWebgl.set(true);
      return;
    }
    this.construir(canvas, this.renderer);
  }

  private construir(canvas: HTMLCanvasElement, renderer: THREE.WebGLRenderer) {
    const reducido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const velocidad = reducido ? 0.15 : 1;
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    const escena = new THREE.Scene();
    escena.fog = new THREE.FogExp2(0x07060f, 0.035);
    const camara = new THREE.PerspectiveCamera(42, 1, 0.1, 100);
    camara.position.set(0, 3.6, 9.6);
    camara.lookAt(0, 0.6, 0);

    // ---------- luces ----------
    escena.add(new THREE.AmbientLight(0xffffff, 0.45));
    const luzCian = new THREE.PointLight(0x22e4ff, 90, 40);
    const luzMagenta = new THREE.PointLight(0xff2ea6, 90, 40);
    escena.add(luzCian, luzMagenta);
    const luzSuave = new THREE.DirectionalLight(0xffffff, 0.6);
    luzSuave.position.set(3, 8, 5);
    escena.add(luzSuave);

    // ---------- teclado de teclas animadas ----------
    const grupo = new THREE.Group();
    escena.add(grupo);
    const columnas = 15;
    const filas = 5;
    const paso = 0.78;
    const total = columnas * filas;
    const geoTecla = new THREE.BoxGeometry(0.66, 0.3, 0.66);
    const matTecla = new THREE.MeshStandardMaterial({ roughness: 0.35, metalness: 0.25 });
    const teclas = new THREE.InstancedMesh(geoTecla, matTecla, total);
    teclas.instanceMatrix.setUsage(THREE.DynamicDrawUsage);
    grupo.add(teclas);

    const geoBase = new THREE.BoxGeometry(columnas * paso + 0.5, 0.28, filas * paso + 0.5);
    const matBase = new THREE.MeshStandardMaterial({ color: 0x0b0a1a, roughness: 0.6, metalness: 0.7 });
    const base = new THREE.Mesh(geoBase, matBase);
    base.position.y = -0.28;
    grupo.add(base);
    grupo.rotation.set(-0.12, 0.35, 0);
    grupo.position.set(0, -1.2, 0);

    // ---------- nudo de neón ----------
    const geoNudo = new THREE.TorusKnotGeometry(0.95, 0.28, 180, 24);
    const matNudo = new THREE.MeshStandardMaterial({
      color: 0x8b5cf6, emissive: 0x5b2ee0, emissiveIntensity: 0.9, metalness: 0.75, roughness: 0.2,
    });
    const nudo = new THREE.Mesh(geoNudo, matNudo);
    nudo.position.set(0, 2.9, -1.2);
    escena.add(nudo);

    // ---------- malla geométrica ----------
    const geoMalla = new THREE.IcosahedronGeometry(4.6, 1);
    const matMalla = new THREE.MeshBasicMaterial({ color: 0x22e4ff, wireframe: true, transparent: true, opacity: 0.1 });
    const malla = new THREE.Mesh(geoMalla, matMalla);
    malla.position.set(0, 1.4, -2);
    escena.add(malla);

    // ---------- partículas ----------
    const n = 800;
    const posiciones = new Float32Array(n * 3);
    const colores = new Float32Array(n * 3);
    const c1 = new THREE.Color(0x22e4ff);
    const c2 = new THREE.Color(0xff2ea6);
    for (let i = 0; i < n; i++) {
      const r = 6 + Math.random() * 12;
      const a = Math.random() * Math.PI * 2;
      const b = Math.acos(2 * Math.random() - 1);
      posiciones[i * 3] = r * Math.sin(b) * Math.cos(a);
      posiciones[i * 3 + 1] = r * Math.cos(b) * 0.6;
      posiciones[i * 3 + 2] = r * Math.sin(b) * Math.sin(a) - 4;
      const mezcla = c1.clone().lerp(c2, Math.random());
      colores.set([mezcla.r, mezcla.g, mezcla.b], i * 3);
    }
    const geoParticulas = new THREE.BufferGeometry();
    geoParticulas.setAttribute('position', new THREE.BufferAttribute(posiciones, 3));
    geoParticulas.setAttribute('color', new THREE.BufferAttribute(colores, 3));
    const matParticulas = new THREE.PointsMaterial({ size: 0.07, vertexColors: true, transparent: true, opacity: 0.85, depthWrite: false });
    const particulas = new THREE.Points(geoParticulas, matParticulas);
    escena.add(particulas);

    // ---------- interacción ----------
    const raton = { x: 0, y: 0 };
    const alMover = (e: PointerEvent) => {
      raton.x = (e.clientX / window.innerWidth) * 2 - 1;
      raton.y = (e.clientY / window.innerHeight) * 2 - 1;
    };
    window.addEventListener('pointermove', alMover, { passive: true });
    this.limpiar.push(() => window.removeEventListener('pointermove', alMover));

    // ---------- tamaño y visibilidad ----------
    const ajustar = () => {
      const caja = canvas.parentElement!.getBoundingClientRect();
      const w = Math.max(caja.width, 1);
      const h = Math.max(caja.height, 1);
      renderer.setSize(w, h, false);
      camara.aspect = w / h;
      camara.position.z = w / h < 1 ? 13 : 9.6;
      camara.updateProjectionMatrix();
    };
    ajustar();
    const observadorTamano = new ResizeObserver(ajustar);
    observadorTamano.observe(canvas.parentElement!);
    const observadorVista = new IntersectionObserver(([e]) => (this.visible = e.isIntersecting));
    observadorVista.observe(canvas);
    this.limpiar.push(() => { observadorTamano.disconnect(); observadorVista.disconnect(); });

    // ---------- bucle de animación ----------
    const matriz = new THREE.Object3D();
    const color = new THREE.Color();
    const reloj = new THREE.Clock();
    const offX = ((columnas - 1) * paso) / 2;
    const offZ = ((filas - 1) * paso) / 2;
    const rotObjetivo = { x: 0, y: 0 };

    const animar = () => {
      this.frame = requestAnimationFrame(animar);
      if (!this.visible || document.hidden) return;
      const t = reloj.getElapsedTime() * velocidad;

      let i = 0;
      for (let f = 0; f < filas; f++) {
        for (let c = 0; c < columnas; c++) {
          const x = c * paso - offX;
          const z = f * paso - offZ;
          const ola = Math.sin(t * 1.7 + x * 0.55 + z * 0.45) * 0.22;
          matriz.position.set(x, ola, z);
          matriz.updateMatrix();
          teclas.setMatrixAt(i, matriz.matrix);
          const tono = 0.5 + 0.38 * Math.sin(t * 0.45 + x * 0.17 + z * 0.22);
          color.setHSL(tono > 0.69 ? 0.88 : tono, 0.92, 0.35 + (ola + 0.22) * 0.55);
          teclas.setColorAt(i, color);
          i++;
        }
      }
      teclas.instanceMatrix.needsUpdate = true;
      if (teclas.instanceColor) teclas.instanceColor.needsUpdate = true;

      nudo.rotation.x = t * 0.5;
      nudo.rotation.y = t * 0.7;
      nudo.position.y = 2.9 + Math.sin(t * 1.1) * 0.25;
      malla.rotation.y = t * 0.12;
      malla.rotation.x = t * 0.06;
      particulas.rotation.y = t * 0.03;

      luzCian.position.set(Math.cos(t * 0.8) * 7, 4 + Math.sin(t) * 1.5, Math.sin(t * 0.8) * 5);
      luzMagenta.position.set(Math.cos(t * 0.8 + Math.PI) * 7, 3, Math.sin(t * 0.8 + Math.PI) * 5);

      rotObjetivo.y = raton.x * 0.45;
      rotObjetivo.x = raton.y * 0.18;
      grupo.rotation.y += (0.35 + rotObjetivo.y - grupo.rotation.y) * 0.06;
      grupo.rotation.x += (-0.12 + rotObjetivo.x - grupo.rotation.x) * 0.06;
      nudo.position.x += (raton.x * 0.9 - nudo.position.x) * 0.05;

      renderer.render(escena, camara);
    };
    animar();

    this.limpiar.push(() => {
      cancelAnimationFrame(this.frame);
      [geoTecla, geoBase, geoNudo, geoMalla, geoParticulas].forEach((g) => g.dispose());
      [matTecla, matBase, matNudo, matMalla, matParticulas].forEach((m) => m.dispose());
      teclas.dispose();
      renderer.dispose();
    });
  }

  ngOnDestroy() {
    this.limpiar.forEach((fn) => fn());
    this.limpiar = [];
  }
}
