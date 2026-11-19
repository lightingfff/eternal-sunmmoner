package com.example.game.diorama

internal object CourtyardShaders {
  val shadowVertex = """#version 300 es
    layout(location=0) in vec3 point;
    uniform mat4 sunMatrix;
    void main() { gl_Position = sunMatrix * vec4(point, 1.0); }
  """.trimIndent()

  val shadowFragment = """#version 300 es
    precision mediump float;
    void main() {}
  """.trimIndent()

  val worldVertex = """#version 300 es
    layout(location=0) in vec3 point;
    layout(location=1) in vec3 normal;
    layout(location=2) in vec2 uv;
    layout(location=3) in vec3 color;
    uniform mat4 viewProjection;
    uniform mat4 sunMatrix;
    out vec3 position;
    out vec3 surfaceNormal;
    out vec2 texCoord;
    out vec3 tint;
    out vec4 sunPosition;
    void main() {
      position = point;
      surfaceNormal = normal;
      texCoord = uv;
      tint = color;
      sunPosition = sunMatrix * vec4(point, 1.0);
      gl_Position = viewProjection * vec4(point, 1.0);
    }
  """.trimIndent()

  // The RGBA8 path compands linear radiance so values over 1 survive on ES 3.0
  // devices where RGBA16F is not color-renderable. All math uses decoded light.
  private val radiance = """
    uniform int packedRadiance;
    vec3 storeLight(vec3 light) {
      return packedRadiance == 0 ? light : light / (1.0 + light);
    }
    vec3 readLight(sampler2D image, vec2 p) {
      vec3 value = texture(image, p).rgb;
      return packedRadiance == 0 ? value : value / max(1.0 - value, vec3(.004));
    }
    float luminance(vec3 light) { return dot(light, vec3(.2126, .7152, .0722)); }
  """.trimIndent()

  val worldFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D material;
    uniform sampler2D sunDepth;
    uniform sampler2D heroAtlas;
    uniform vec4 heroFrame;
    uniform vec3 heroSide;
    uniform vec2 heroHeight;
    uniform vec3 camera;
    uniform vec3 actor;
    uniform vec3 sunDirection;
    uniform int kind;
    uniform float timer;
    uniform vec3 lamps[4];
    in vec3 position;
    in vec3 surfaceNormal;
    in vec2 texCoord;
    in vec3 tint;
    in vec4 sunPosition;
    out vec4 pixel;

    float visibility(float cosine) {
      vec3 uvz = sunPosition.xyz / sunPosition.w * 0.5 + 0.5;
      if (any(lessThan(uvz, vec3(0.0))) || any(greaterThan(uvz, vec3(1.0)))) return 1.0;
      float bias = mix(.00192, .00067, cosine);
      vec2 texel = 1.0 / vec2(textureSize(sunDepth, 0));
      float reference = uvz.z - bias;
      vec2 spread = texel * 1.35;
      float visible = 2.0 * step(reference, texture(sunDepth, uvz.xy).r);
      visible += step(reference, texture(sunDepth, uvz.xy + spread).r);
      visible += step(reference, texture(sunDepth, uvz.xy - spread).r);
      visible += step(reference, texture(sunDepth, uvz.xy + vec2(-spread.x, spread.y)).r);
      visible += step(reference, texture(sunDepth, uvz.xy + vec2(spread.x, -spread.y)).r);
      return visible / 6.0;
    }

    float actorShadow(vec2 delta) {
      if (dot(delta, delta) > 13.0) return 0.0;
      vec2 side = heroSide.xy;
      vec2 forward = vec2(-side.y, side.x);
      float approach = dot(sunDirection.xz, forward);
      if (abs(approach) < .08) return 0.0;
      float ray = -dot(delta, forward) / approach;
      if (ray <= 0.0) return 0.0;
      vec2 k = vec2(dot(delta + sunDirection.xz * ray, side) / (2.0 * heroSide.z) + .5,
        (position.y + ray * sunDirection.y - heroHeight.x) / (heroHeight.y - heroHeight.x));
      if (any(lessThan(k, vec2(0.0))) || any(greaterThan(k, vec2(1.0)))) return 0.0;
      vec2 uv = mix(heroFrame.xy, heroFrame.zw, k);
      vec2 blur = vec2(.00135, .0) * mix(.68, 1.30, k.y);
      float mask = texture(heroAtlas, uv).a * .50
        + texture(heroAtlas, uv - blur).a * .25 + texture(heroAtlas, uv + blur).a * .25;
      return smoothstep(.08, .91, mask);
    }

    vec3 lamplight(vec3 n) {
      float spill = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec3 ray = lamps[i] - position;
        float square = dot(ray, ray);
        if (square >= 7.2) continue;
        float falloff = 1.0 - smoothstep(0.08, 7.2, square);
        spill += falloff * falloff * max(0.0, dot(n, ray) * inversesqrt(max(square, .04)));
      }
      return vec3(1.1, .44, .12) * spill;
    }

    void main() {
      vec2 surfaceUv = kind == 1 ? position.xz * .62 : texCoord;
      vec4 source = texture(material, surfaceUv);
      if (source.a < .45) discard;
      vec3 base = pow(max(source.rgb * tint, vec3(.001)), vec3(2.2));
      vec3 n = normalize(surfaceNormal);
      float lambert = max(dot(n, sunDirection), 0.0);
      float shadow = visibility(lambert);
      vec3 ambient = mix(vec3(.22, .21, .22), vec3(.32, .43, .57), .5 + .5 * n.y);
      vec3 sunlight = vec3(4.04, 3.12, 2.15) * lambert * shadow;
      if (kind == 1) {
        vec2 dx = (position.xz - actor.xz) / vec2(.38, .28);
        float contact = exp(-dot(dx, dx) * 1.7) * (1.0 - smoothstep(.035, .26, abs(position.y - actor.y)));
        ambient *= 1.0 - .50 * contact;
        float silhouette = actorShadow(position.xz - actor.xz);
        sunlight *= 1.0 - max(.88 * contact, .93 * silhouette);
      } else if (kind == 2) {
        float t = timer * .46;
        float a = position.z * 5.1 + sin(position.x * 1.64) + t;
        float b = position.x * 3.8 - position.z * 2.7 + t * .71;
        n = normalize(vec3(.15 * cos(b), 1.0, .24 * cos(a)));
        vec3 viewDirection = normalize(camera - position);
        float fresnel = pow(1.0 - max(0.0, dot(n, viewDirection)), 3.0);
        float glint = pow(max(dot(n, normalize(sunDirection + viewDirection)), 0.0), 21.0);
        base = mix(base, vec3(.10, .25, .28), fresnel * .43);
        sunlight = vec3(.82, .87, .82) + vec3(1.32, 1.09, .66) * glint * shadow;
      } else if (kind == 3) {
        ambient = vec3(.95, .96, 1.04);
        sunlight = vec3(1.48, 1.13, .73) * shadow;
      }
      vec3 lit = base * (ambient + sunlight + lamplight(n));
      if (kind == 4) lit = base * 4.8;
      float distanceToEye = distance(camera, position);
      float haze = smoothstep(16.0, 48.0, distanceToEye) * .46;
      float coolGroundFog = (1.0 - smoothstep(-.8, 2.0, position.y)) * smoothstep(19.0, 44.0, distanceToEye);
      vec3 air = mix(vec3(.19, .30, .38), vec3(.41, .40, .31), shadow * .46);
      pixel = vec4(storeLight(mix(lit, air, haze + (1.0 - haze) * coolGroundFog * .32)), 1.0);
    }
  """.trimIndent()

  val lensVertex = """#version 300 es
    out vec2 uv;
    void main() {
      vec2 corner = vec2(gl_VertexID == 1 ? 3.0 : -1.0, gl_VertexID == 2 ? 3.0 : -1.0);
      uv = corner * .5 + .5;
      gl_Position = vec4(corner, 0.0, 1.0);
    }
  """.trimIndent()

  val bloomFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D rendered;
    uniform vec2 sampleStep;
    uniform int extract;
    in vec2 uv;
    out vec4 pixel;

    vec3 highlights(vec2 p) {
      vec3 color = readLight(rendered, p);
      float value = luminance(color);
      return color * smoothstep(.83, 2.12, value);
    }

    void main() {
      vec3 light;
      if (extract == 1) {
        vec2 d = sampleStep * 1.15;
        light = (highlights(uv + d) + highlights(uv - d) +
          highlights(uv + vec2(d.x, -d.y)) + highlights(uv + vec2(-d.x, d.y))) * .25;
      } else {
        light = readLight(rendered, uv) * .227027;
        light += (readLight(rendered, uv + sampleStep * 1.384615) +
          readLight(rendered, uv - sampleStep * 1.384615)) * .316216;
        light += (readLight(rendered, uv + sampleStep * 3.230769) +
          readLight(rendered, uv - sampleStep * 3.230769)) * .070270;
      }
      pixel = vec4(storeLight(light), 1.0);
    }
  """.trimIndent()

  val lensFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D rendered;
    uniform sampler2D depth;
    uniform sampler2D glow;
    uniform sampler2D fogSunDepth;
    uniform mat4 inverseViewProjection;
    uniform mat4 sunMatrix;
    uniform vec3 camera;
    uniform vec2 pixelStep;
    uniform vec2 planes;
    uniform float focus;
    in vec2 uv;
    out vec4 pixel;

    const vec2 blurRing[6] = vec2[6](
      vec2(1.0, 0.0), vec2(.5, .866), vec2(-.5, .866),
      vec2(-1.0, 0.0), vec2(-.5, -.866), vec2(.5, -.866)
    );

    float distanceAt(float d) {
      return (planes.x * planes.y) / mix(planes.y, planes.x, d);
    }

    vec3 colorAt(vec2 p, float d) {
      if (d < .99999) return readLight(rendered, p);
      vec3 sky = mix(vec3(.52, .49, .38), vec3(.21, .32, .42), smoothstep(.22, 1.0, p.y));
      vec2 ray = (p - vec2(.07, 1.03)) * vec2(2.2, 3.4);
      return sky + vec3(.27, .20, .10) * exp(-dot(ray, ray));
    }

    vec3 sunHaze(float sceneDepth) {
      vec2 ndc = uv * 2.0 - 1.0;
      vec4 clip = inverseViewProjection * vec4(ndc, sceneDepth * 2.0 - 1.0, 1.0);
      vec3 segment = clip.xyz / clip.w - camera;
      float rayLength = length(segment);
      vec3 ray = segment / max(rayLength, .001);
      float end = min(rayLength, 38.0);
      float scatter = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec3 p = camera + ray * end * (float(i) + .5) * .25;
        // Limit scattering to the stream and the gap between roofs.
        vec2 axis = (p.xz + p.y * vec2(1.5, -.94) - vec2(3.0, -2.5)) * vec2(.31, .161);
        float density = exp(-dot(axis, axis)) * (1.0 - smoothstep(1.6, 8.0, p.y));
        if (density < .006) continue;
        vec3 lightUv = (sunMatrix * vec4(p, 1.0)).xyz * .5 + .5;
        float lit = step(lightUv.z - .0012, texture(fogSunDepth, lightUv.xy).r);
        scatter += density * lit;
      }
      return vec3(.91, .66, .34) * scatter * end * .0027;
    }

    void main() {
      float centerDepth = texture(depth, uv).r;
      float center = distanceAt(centerDepth);
      float defocus = center < focus ? focus - center : (center - focus) * .75;
      float radius = smoothstep(2.20, 11.5, defocus) * 6.6;
      vec3 color = colorAt(uv, centerDepth);
      if (radius > .35) {
        vec3 sum = color * 2.0;
        float weight = 2.0;
        for (int i = 0; i < 6; ++i) {
          vec2 q = uv + blurRing[i] * pixelStep * radius;
          float qDepth = texture(depth, q).r;
          float other = distanceAt(qDepth);
          // Weight similar depths so blurred roofs and railings do not leak onto the player.
          float w = 1.0 - smoothstep(.85, 3.0, abs(other - center));
          sum += colorAt(q, qDepth) * w;
          weight += w;
        }
        color = sum / weight;
      }
      color += readLight(glow, uv) * .28 + sunHaze(centerDepth);
      float edge = smoothstep(.31, .74, length((uv - vec2(.48, .48)) * vec2(1.0, .89)));
      color *= 1.0 - .24 * edge;
      // Tone mapping compresses highlights before conversion from linear light to display color.
      vec3 exposed = color * 1.04;
      color = (exposed * (2.51 * exposed + .03)) / (exposed * (2.43 * exposed + .59) + .14);
      pixel = vec4(pow(clamp(color, 0.0, 1.0), vec3(1.0 / 2.2)), 1.0);
    }
  """.trimIndent()
}
