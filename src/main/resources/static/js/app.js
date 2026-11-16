
const API = '/api';

// -------------------- BASIC HELPERS --------------------

const iconFor = (category = '') => {
  const c = String(category).toLowerCase();

  if (c.includes('program')) return '</>';
  if (c.includes('database')) return '▤';
  if (c.includes('artificial') || c.includes('machine')) return '✳';
  if (c.includes('development')) return '⌘';

  return '✦';
};

const escapeHtml = value =>
  String(value ?? '').replace(/[&<>"']/g, ch => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[ch]));

async function api(url, options = {}) {
  const response = await fetch(API + url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    let message = 'Something went wrong. Please try again.';

    try {
      const data = await response.json();
      message = data.message || data.error || message;
    } catch (error) {
      // Keep the default error message.
    }

    throw new Error(message);
  }

  if (response.status === 204) return null;

  return response.json();
}

// -------------------- STUDENT SESSION --------------------

function currentStudent() {
  try {
    const student = JSON.parse(
      localStorage.getItem('learnhubStudent') || 'null'
    );

    return student && student.id && student.name && student.email
      ? student
      : null;
  } catch (error) {
    return null;
  }
}

// -------------------- COURSE CARD --------------------

function courseCard(course, action = '') {
  const category = course.category || 'General';
  const icon = iconFor(category);

  return `
    <article class="course-card reveal-card">
      <div class="course-icon">${icon}</div>

      <div class="course-category">
        ${escapeHtml(category)}
      </div>

      <h3>${escapeHtml(course.title)}</h3>

      <p>
        ${escapeHtml(
          course.description ||
          'Explore this course and develop new skills.'
        )}
      </p>

      <div class="course-meta">
        <span>
          ◷ ${escapeHtml(course.durationWeeks ?? '?')} weeks
        </span>

        <span>
          ✦ ${escapeHtml(course.instructor || 'SkillSphere')}
        </span>

        ${
          action ||
          `<span>${escapeHtml(course.enrolledCount ?? 0)} enrolled</span>`
        }
      </div>
    </article>
  `;
}

// -------------------- ENROLLMENT MODAL --------------------

function ensureCourseModal() {
  if (document.getElementById('course-modal')) return;

  document.body.insertAdjacentHTML('beforeend', `
    <div class="modal-backdrop" id="course-modal" aria-hidden="true">

      <section
        class="modal-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="course-modal-title"
      >

        <button
          class="modal-close"
          type="button"
          aria-label="Close dialog"
        >×</button>

        <div class="modal-spark">✦</div>

        <div class="eyebrow">SKILLSPHERE COURSE</div>

        <h2 id="course-modal-title">Ready to begin?</h2>

        <p id="course-modal-copy">
          Join this learning path and track your progress.
        </p>

        <div class="modal-actions">

          <button
            class="btn btn-outline modal-cancel"
            type="button"
          >
            Maybe later
          </button>

          <button
            class="btn modal-confirm"
            type="button"
          >
            Confirm enrolment ↗
          </button>

        </div>
      </section>
    </div>
  `);

  const modal = document.getElementById('course-modal');

  const close = () => {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  };

  modal.querySelectorAll('.modal-close, .modal-cancel')
    .forEach(button => {
      button.addEventListener('click', close);
    });

  modal.addEventListener('click', event => {
    if (event.target === modal) close();
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') close();
  });
}

function showCourseModal(course, onConfirm, success = false) {
  ensureCourseModal();

  const modal = document.getElementById('course-modal');

  modal.querySelector('#course-modal-title').textContent =
    success ? 'You’re enrolled!' : `Start ${course.title}?`;

  modal.querySelector('#course-modal-copy').textContent =
    success
      ? 'Your course has been added to your learning dashboard.'
      : 'Confirm to add this course to your learning dashboard.';

  const confirm = modal.querySelector('.modal-confirm');

  confirm.textContent = success
    ? 'Go to dashboard ↗'
    : 'Confirm enrolment ↗';

  confirm.disabled = false;

  confirm.onclick = success
    ? () => {
        location.href = '/dashboard.html';
      }
    : onConfirm;

  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
}

// -------------------- COURSE LIST --------------------

async function loadCourses(targetId, searchable = false) {
  const target = document.getElementById(targetId);

  if (!target) return;

  try {
    const courses = await api('/courses');

    const student = currentStudent();

    let enrolledIds = new Set();

    if (student) {
      try {
        const mine = await api(
          `/students/${student.id}/enrollments`
        );

        enrolledIds = new Set(
          mine.map(item => Number(item.courseId))
        );
      } catch (error) {
        console.warn('Could not load enrollment status:', error);
      }
    }

    const render = list => {
      target.innerHTML = list.length
        ? list.map(course => {
            const enrolled = enrolledIds.has(Number(course.id));

            const action = enrolled
              ? `
                <button
                  type="button"
                  class="enrolled-button"
                  disabled
                >
                  ✓ Enrolled
                </button>
              `
              : `
                <button
                  type="button"
                  data-course-id="${escapeHtml(course.id)}"
                  class="enroll-button"
                >
                  Explore course ↗
                </button>
              `;

            return courseCard(course, action);
          }).join('')
        : '<div class="empty-state">No courses match your search.</div>';

      // Enrollment button events
      target.querySelectorAll('.enroll-button')
        .forEach(button => {
          button.addEventListener('click', () => {
            const course = courses.find(
              item =>
                Number(item.id) === Number(button.dataset.courseId)
            );

            if (!course) return;

            if (!currentStudent()) {
              location.href = '/login.html';
              return;
            }

            showCourseModal(course, async () => {
              const confirm = document.querySelector('.modal-confirm');

              confirm.disabled = true;
              confirm.textContent = 'Adding course...';

              try {
                const loggedInStudent = currentStudent();

                if (!loggedInStudent) {
                  location.href = '/login.html';
                  return;
                }

                await api(
                  `/students/${loggedInStudent.id}/enrollments/${course.id}`,
                  { method: 'POST' }
                );

                enrolledIds.add(Number(course.id));

                filterCourses();

                showCourseModal(course, null, true);

              } catch (error) {
                confirm.disabled = false;
                confirm.textContent = 'Try again';

                document.getElementById(
                  'course-modal-copy'
                ).textContent = error.message;
              }
            });
          });
        });

      // Card animation
      target.querySelectorAll('.reveal-card')
        .forEach((card, index) => {
          card.style.animationDelay =
            `${Math.min(index * 55, 350)}ms`;
        });
    };

    // Search and category filtering
    const count = document.getElementById('course-count');
    const searchInput = searchable
      ? document.getElementById('course-search')
      : null;

    const categorySelect = searchable
      ? document.getElementById('course-category')
      : null;

    const filterCourses = () => {
      const query = (searchInput?.value || '')
        .toLowerCase()
        .trim();

      const selectedCategory =
        categorySelect?.value || 'all';

      const filtered = courses.filter(course => {
        const searchableText = [
          course.title,
          course.category,
          course.description,
          course.instructor
        ].join(' ').toLowerCase();

        const matchesSearch = searchableText.includes(query);

        const matchesCategory =
          selectedCategory === 'all' ||
          course.category === selectedCategory;

        return matchesSearch && matchesCategory;
      });

      render(filtered);

      if (count) {
        count.textContent =
          `${filtered.length} of ${courses.length} courses`;
      }
    };

    if (categorySelect) {
      const categories = [
        ...new Set(
          courses.map(course => course.category).filter(Boolean)
        )
      ].sort();

      categorySelect.innerHTML =
        '<option value="all">All categories</option>' +
        categories.map(category => `
          <option value="${escapeHtml(category)}">
            ${escapeHtml(category)}
          </option>
        `).join('');
    }

    render(courses);

    if (count) {
      count.textContent = `${courses.length} courses available`;
    }

    if (searchable) {
      searchInput?.addEventListener('input', filterCourses);
      categorySelect?.addEventListener('change', filterCourses);
    }

  } catch (error) {
    target.innerHTML = `
      <div class="empty-state">
        Unable to load courses.
        Start the Spring Boot server and check your MySQL connection.
        <br>
        ${escapeHtml(error.message)}
      </div>
    `;
  }
}

// -------------------- WELCOME LAMP --------------------

const lampIntro = document.getElementById('lamp-intro');
const lampPull = document.getElementById('lamp-pull');

if (lampIntro && lampPull) {
  if (currentStudent()) {
    location.replace('/dashboard.html');
  } else {
    const loginPanel =
      document.getElementById('lamp-login-panel');
    const instruction =
      document.getElementById('lamp-instruction');
    const prefersReducedMotion =
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const revealLogin = () => {
      lampIntro.classList.add('show-login');

      if (loginPanel) {
        loginPanel.setAttribute('aria-hidden', 'false');

        const firstField = loginPanel.querySelector('input');

        if (firstField) {
          window.setTimeout(
            () => firstField.focus(),
            prefersReducedMotion ? 0 : 650
          );
        }
      }

      if (instruction) {
        instruction.textContent =
          'The light is on. Sign in below to continue.';
      }
    };

    lampPull.addEventListener('click', () => {
      if (lampIntro.classList.contains('chain-pulled')) return;

      lampIntro.classList.add('chain-pulled');
      lampPull.disabled = true;
      lampPull.setAttribute('aria-label', 'Turning on the lamp');

      if (prefersReducedMotion) {
        lampIntro.classList.add('is-lit');
        lampPull.setAttribute('aria-label', 'Lamp switched on');
        revealLogin();
        return;
      }

      lampIntro.classList.add('is-lighting');

      window.setTimeout(() => {
        lampIntro.classList.add('is-lit');
        lampPull.setAttribute('aria-label', 'Lamp switched on');
        if (instruction) {
          instruction.textContent =
            'The future looks brighter. Welcome to SkillSphere!';
        }
        window.setTimeout(revealLogin, 550);
      }, 900);
    });
  }
}

// -------------------- NAVIGATION --------------------

function updateAuthNavigation() {
  const student = currentStudent();

  document.querySelectorAll('.nav-actions').forEach(nav => {
    const login = nav.querySelector('a[href="/login.html"]');
    const register = nav.querySelector('a[href="/register.html"]');

    if (student) {
      if (login) {
        login.href = '/dashboard.html';
        login.textContent = 'Dashboard';
      }

      if (register) register.remove();
    }
  });
}

if (
  currentStudent() &&
  (
    location.pathname === '/login.html' ||
    location.pathname === '/register.html'
  )
) {
  location.replace('/dashboard.html');
}

updateAuthNavigation();

// -------------------- REGISTER --------------------

const registerForm = document.getElementById('register-form');

if (registerForm) {
  registerForm.addEventListener('submit', async event => {
    event.preventDefault();

    const form = new FormData(registerForm);
    const message = document.getElementById('register-message');

    message.className = 'form-message';
    message.textContent = 'Creating your account...';

    try {
      const student = await api('/students/register', {
        method: 'POST',
        body: JSON.stringify({
          name: form.get('name'),
          email: form.get('email'),
          password: form.get('password')
        })
      });

      localStorage.setItem(
        'learnhubStudent',
        JSON.stringify(student)
      );

      message.className = 'form-message success';
      message.textContent =
        'Account created. Opening your dashboard...';

      setTimeout(() => {
        location.href = '/dashboard.html';
      }, 650);

    } catch (error) {
      message.className = 'form-message error';
      message.textContent = error.message;
    }
  });
}

// -------------------- LOGIN --------------------

const loginForm = document.getElementById('login-form');

if (loginForm) {
  loginForm.addEventListener('submit', async event => {
    event.preventDefault();

    const form = new FormData(loginForm);
    const message = document.getElementById('login-message');

    message.className = 'form-message';
    message.textContent = 'Signing you in...';

    try {
      const student = await api('/students/login', {
        method: 'POST',
        body: JSON.stringify({
          email: form.get('email'),
          password: form.get('password')
        })
      });

      localStorage.setItem(
        'learnhubStudent',
        JSON.stringify(student)
      );

      message.className = 'form-message success';
      message.textContent =
        'Login successful. Opening your dashboard...';

      setTimeout(() => {
        location.href = '/dashboard.html';
      }, 500);

    } catch (error) {
      message.className = 'form-message error';
      message.textContent = error.message;
    }
  });
}

// -------------------- LOGOUT --------------------

const logout = document.getElementById('logout-button');

if (logout) {
  logout.addEventListener('click', () => {
    localStorage.removeItem('learnhubStudent');
    location.href = '/';
  });
}

// -------------------- DASHBOARD --------------------

async function loadDashboard() {
  const student = currentStudent();

  const grid = document.getElementById('my-courses');

  if (!grid) return;

  if (!student) {
    location.href = '/login.html';
    return;
  }

  const studentName = document.getElementById('student-name');
  const studentEmail = document.getElementById('student-email');
  const enrolledTotal = document.getElementById('enrolled-total');

  if (studentName) {
    studentName.textContent = student.name.split(' ')[0];
  }

  if (studentEmail) {
    studentEmail.textContent = student.email;
  }

  try {
    const courses = await api(
      `/students/${student.id}/enrollments`
    );

    if (enrolledTotal) {
      enrolledTotal.textContent = courses.length;
    }

    grid.innerHTML = courses.length
      ? courses.map(course =>
          courseCard(
            course,
            `
              <button
                type="button"
                class="unenroll-button"
                data-course-id="${escapeHtml(course.courseId)}"
              >
                Leave course
              </button>
            `
          )
        ).join('')
      : `
        <div class="empty-state">
          You have not enrolled in a course yet.
          <a class="text-link" href="/courses.html">
            Explore courses ↗
          </a>
        </div>
      `;

    grid.querySelectorAll('.unenroll-button')
      .forEach(button => {
        button.addEventListener('click', async () => {
          button.disabled = true;

          try {
            await api(
              `/students/${student.id}/enrollments/${button.dataset.courseId}`,
              { method: 'DELETE' }
            );

            loadDashboard();

          } catch (error) {
            button.disabled = false;

            const notice =
              document.getElementById('dashboard-notice');

            if (notice) notice.textContent = error.message;
          }
        });
      });

  } catch (error) {
    grid.innerHTML = `
      <div class="empty-state">
        ${escapeHtml(error.message)}
      </div>
    `;
  }
}

// -------------------- LOAD PAGE DATA --------------------

loadCourses('home-course-grid');
loadCourses('course-grid', true);
loadDashboard();

// -------------------- AI STUDY LAB --------------------

const aiForm = document.getElementById('ai-form');

if (aiForm) {
  const promptBox = document.getElementById('ai-prompt');
  const result = document.getElementById('ai-result');
  const status = document.getElementById('ai-status');

  const runAI = async prompt => {
    if (!prompt.trim()) {
      status.className = 'ai-status';
      status.textContent =
        'Enter a topic or some notes first.';
      return;
    }

    status.className = 'ai-status loading';
    status.textContent = 'AI is preparing your answer...';
    result.textContent = '';

    const submitBtn =
      aiForm.querySelector('button[type="submit"]');
    const toolButtons =
      document.querySelectorAll('[data-ai-tool]');

    if (submitBtn) submitBtn.disabled = true;
    toolButtons.forEach(btn => (btn.disabled = true));

    try {
      const data = await api('/ai/ask', {
        method: 'POST',
        body: JSON.stringify({ prompt })
      });

      result.textContent = data.answer;
      status.className = 'ai-status success';
      status.textContent =
        'Ready · Review the answer and ask a follow-up.';

    } catch (error) {
      status.className = 'ai-status error';
      status.textContent = error.message;

    } finally {
      if (submitBtn) submitBtn.disabled = false;
      toolButtons.forEach(btn => (btn.disabled = false));
    }
  };

  aiForm.addEventListener('submit', event => {
    event.preventDefault();

    runAI(
      `You are LearnHub's friendly academic tutor.
Explain clearly for a beginner, use headings and examples,
and keep the answer accurate.

${promptBox.value}`
    );
  });

  document.querySelectorAll('[data-ai-tool]')
    .forEach(button => {
      button.addEventListener('click', () => {
        const topic = promptBox.value.trim();

        if (!topic) {
          status.textContent =
            'First enter a topic or paste your notes above.';

          promptBox.focus();
          return;
        }

        const kind = button.dataset.aiTool;

        const prompts = {
          explain:
            `Teach me this topic from beginner level with a simple definition, step-by-step explanation, a real-world example, and a short recap: ${topic}`,

          summarize:
            `Summarize these study notes into clear headings and concise bullet points. Preserve important facts and definitions: ${topic}`,

          quiz:
            `Create 5 multiple-choice questions from this topic/notes. Give 4 options per question, mark the correct answer, and explain why: ${topic}`,

          flashcards:
            `Create 8 concise question-and-answer flashcards from this topic/notes. Format as Front: ... and Back: ...: ${topic}`,

          plan:
            `Create a realistic 7-day beginner study plan for this topic. Include daily goals, estimated time, practice, and a revision day: ${topic}`
        };

        runAI(prompts[kind] || prompts.explain);
      });
    });
}
